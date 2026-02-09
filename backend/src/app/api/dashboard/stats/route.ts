import { NextResponse } from "next/server";
import { getAdminFirestore } from "@/lib/firebase-admin";
import type {
  DashboardStats,
  UserDistribution,
  FightStatusDistribution,
  RecentFight,
  FightStatus,
} from "@/types/dashboard";


async function getUserDistribution(): Promise<UserDistribution> {
  const db = getAdminFirestore();
  const usersRef = db.collection("users");
  const snapshot = await usersRef.get();

  let clients = 0;
  let fighters = 0;

  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    const role = data.role?.toUpperCase() || "CLIENT";

    if (role === "FIGHTER") {
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
  const fightsRef = db.collection("fights");
  const snapshot = await fightsRef.get();

  const distribution: FightStatusDistribution = {
    pending: 0,
    inProgress: 0,
    completed: 0,
    cancelled: 0,
    total: 0,
  };

  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    const status = (data.status?.toUpperCase() || "PENDING") as FightStatus;

    switch (status) {
      case "PENDING":
        distribution.pending++;
        break;
      case "IN_PROGRESS":
        distribution.inProgress++;
        break;
      case "COMPLETED":
        distribution.completed++;
        break;
      case "CANCELLED":
        distribution.cancelled++;
        break;
    }
    distribution.total++;
  });

  return distribution;
}

async function getAverageRating(): Promise<number> {
  const db = getAdminFirestore();
  const usersRef = db.collection("users");
  const snapshot = await usersRef.where("role", "==", "FIGHTER").get();

  if (snapshot.empty) {
    return 0;
  }

  let totalRating = 0;
  let ratedFighters = 0;

  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    if (typeof data.rating === "number" && data.rating > 0) {
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
  const fightsRef = db.collection("fights");
  const snapshot = await fightsRef
    .orderBy("createdAt", "desc")
    .limit(limitCount)
    .get();

  const fights: RecentFight[] = [];

  for (const doc of snapshot.docs) {
    const data = doc.data();

    let locationString: string | undefined;
    if (data.location) {
      if (typeof data.location === "string") {
        locationString = data.location;
      } else if (data.location._latitude !== undefined && data.location._longitude !== undefined) {
        locationString = `${data.location._latitude.toFixed(4)}, ${data.location._longitude.toFixed(4)}`;
      } else if (data.location.latitude !== undefined && data.location.longitude !== undefined) {
        locationString = `${data.location.latitude.toFixed(4)}, ${data.location.longitude.toFixed(4)}`;
      }
    }

    fights.push({
      id: doc.id,
      clientName: data.clientName || "Client inconnu",
      fighterName: data.fighterName || "Bagarreur inconnu",
      status: (data.status?.toUpperCase() || "PENDING") as FightStatus,
      createdAt: data.createdAt?.toDate?.()?.toISOString() || new Date().toISOString(),
      location: locationString,
    });
  }

  return fights;
}

export async function GET() {
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
    console.error("Erreur lors de la récupération des stats dashboard:", error);

    return NextResponse.json(
      {
        error: "Erreur serveur lors de la récupération des statistiques",
        details: error instanceof Error ? error.message : "Erreur inconnue",
      },
      { status: 500 }
    );
  }
}
