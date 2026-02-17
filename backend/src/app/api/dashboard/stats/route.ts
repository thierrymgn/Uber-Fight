import { NextResponse } from 'next/server';
import { getAdminFirestore } from '@/lib/firebase-admin';
import { verifyAuth } from '@/lib/auth-admin';
import type {
  DashboardStats,
  UserDistribution,
  FightStatusDistribution,
  RecentFight,
  FightStatus,
} from '@/types/dashboard';
import {
  FirebaseError,
  logFirebaseError,
  withPerformanceLogging,
  withApiMetrics,
} from '@/lib/grafana';

async function getUserDistribution(): Promise<UserDistribution> {
  const db = getAdminFirestore();
  let snapshot;
  try {
    snapshot = await withPerformanceLogging('fetchAllUsers', async () => {
      const usersRef = db.collection('users');

      return await usersRef.get();
    });
  } catch (error) {
    logFirebaseError(error as FirebaseError, 'fetchAllUsers', {});
    return {
      clients: 0,
      fighters: 0,
      total: 0,
    };
  }

  let clients = 0;
  let fighters = 0;

  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    const role = data.role?.toUpperCase() || 'CLIENT';

    if (role === 'FIGHTER') {
      fighters++;
    } else {
      clients++;
    }
  });

  return {
    clients,
    fighters,
    total: clients + fighters,
  };
}

async function getFightStatusDistribution(): Promise<FightStatusDistribution> {
  const db = getAdminFirestore();
  let snapshot;

  try {
    snapshot = await withPerformanceLogging('fetchAllFights', async () => {
      const fightsRef = db.collection('fights');
      return await fightsRef.get();
    });
  } catch (error) {
    logFirebaseError(error as FirebaseError, 'fetchAllFights', {});
    return {
      pending: 0,
      inProgress: 0,
      completed: 0,
      cancelled: 0,
      total: 0,
    };
  }

  const distribution: FightStatusDistribution = {
    pending: 0,
    inProgress: 0,
    completed: 0,
    cancelled: 0,
    total: 0,
  };

  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    const status = (data.status?.toUpperCase() || 'PENDING') as FightStatus;

    switch (status) {
      case 'PENDING':
        distribution.pending++;
        break;
      case 'IN_PROGRESS':
        distribution.inProgress++;
        break;
      case 'COMPLETED':
        distribution.completed++;
        break;
      case 'CANCELLED':
        distribution.cancelled++;
        break;
    }
    distribution.total++;
  });

  return distribution;
}

async function getAverageRating(): Promise<number> {
  const db = getAdminFirestore();

  let snapshot;

  try {
    snapshot = await withPerformanceLogging('fetchAllUsersFilteredByFighter', async () => {
      const usersRef = db.collection('users');

      return await usersRef.where('role', '==', 'FIGHTER').get();
    });
  } catch (error) {
    logFirebaseError(error as FirebaseError, 'fetchAllUsersFilteredByFighter', {
      filter: 'role == FIGHTER',
    });
    return 0;
  }

  if (snapshot.empty) {
    return 0;
  }

  let totalRating = 0;
  let ratedFighters = 0;

  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    if (typeof data.rating === 'number' && data.rating > 0) {
      totalRating += data.rating;
      ratedFighters++;
    }
  });

  if (ratedFighters === 0) {
    return 0;
  }

  return Math.round((totalRating / ratedFighters) * 10) / 10;
}

async function getRecentFights(limitCount: number = 5): Promise<RecentFight[]> {
  const db = getAdminFirestore();
  let snapshot;
  try {
    snapshot = await withPerformanceLogging('fetchRecentFights', async () => {
      const fightsRef = db.collection('fights');
      return await fightsRef.orderBy('createdAt', 'desc').limit(limitCount).get();
    });
  } catch (error) {
    logFirebaseError(error as FirebaseError, 'fetchRecentFights', { limit: limitCount });
    return [];
  }

  const userIds = new Set<string>();
  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    if (data.requesterId) userIds.add(data.requesterId);
    if (data.fighterId) userIds.add(data.fighterId);
  });

  const userNames: Record<string, string> = {};
  if (userIds.size > 0) {
    try {
      const userDocs = await Promise.all(
        Array.from(userIds).map((id) => db.collection('users').doc(id).get())
      );
      userDocs.forEach((userDoc) => {
        if (userDoc.exists) {
          const userData = userDoc.data();
          userNames[userDoc.id] = userData?.username || 'Inconnu';
        }
      });
    } catch (error) {
      logFirebaseError(error as FirebaseError, 'fetchUserNames', { userIds: JSON.stringify(Array.from(userIds)) });
    }
  }

  const fights: RecentFight[] = [];

  for (const doc of snapshot.docs) {
    const data = doc.data();

    let locationString: string | undefined;
    if (data.location) {
      if (typeof data.location === 'string') {
        locationString = data.location;
      } else if (data.location._latitude !== undefined && data.location._longitude !== undefined) {
        locationString = `${data.location._latitude.toFixed(4)}, ${data.location._longitude.toFixed(4)}`;
      } else if (data.location.latitude !== undefined && data.location.longitude !== undefined) {
        locationString = `${data.location.latitude.toFixed(4)}, ${data.location.longitude.toFixed(4)}`;
      }
    }

    const createdAtDate = data.createdAt?.toDate?.() ?? doc.createTime?.toDate?.() ?? null;

    fights.push({
      id: doc.id,
      clientName: userNames[data.requesterId] || 'Client inconnu',
      fighterName: userNames[data.fighterId] || 'Bagarreur inconnu',
      status: (data.status?.toUpperCase() || 'PENDING') as FightStatus,
      createdAt: createdAtDate ? createdAtDate.toISOString() : null,
      location: locationString,
    });
  }

  return fights;
}

export async function GET(request: Request) {
  return withApiMetrics('/api/dashboard/stats', 'GET', async () => {
    const authResult = await verifyAuth(request, ['ADMIN']);

    if (!authResult.success) {
      return NextResponse.json({ error: authResult.error }, { status: authResult.status });
    }

    try {
      const [userDistribution, fightStatusDistribution, averageRating, recentFights] =
        await Promise.all([
          getUserDistribution(),
          getFightStatusDistribution(),
          getAverageRating(),
          getRecentFights(5),
        ]);

      const stats: DashboardStats = {
        userDistribution,
        fightStatusDistribution,
        averageRating,
        recentFights,
      };

      return NextResponse.json(stats, { status: 200 });
    } catch (error) {
      console.error('Erreur lors de la récupération des stats dashboard:', error);

      return NextResponse.json(
        {
          error: 'Erreur serveur lors de la récupération des statistiques',
          details: error instanceof Error ? error.message : 'Erreur inconnue',
        },
        { status: 500 }
      );
    }
  });
}
