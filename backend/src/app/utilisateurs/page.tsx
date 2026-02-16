'use client';

import { useEffect, useState, useMemo, useCallback } from 'react';
import InfoSection from '@/app/utilisateurs/components/info-section';
import { collection, getDocs } from '@firebase/firestore';
import { db } from '@/lib/firebase';
import { useAuth } from '@/components/providers/auth-provider';
import { User, parseUser, isValidUser } from '@/types/user';
import UsersTable from '@/app/utilisateurs/components/users-table';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { AlertCircle, Search, UserPlus, RefreshCw, ArrowUpDown, Trophy } from 'lucide-react';
import CreateUserDialog from '@/app/utilisateurs/components/create-user-dialog';
import Leaderboard from '@/app/utilisateurs/components/leaderboard';

type SortOption = 'default' | 'rating-desc' | 'rating-asc';
import useLogger from '@/hooks/useLogger';

export default function UtilisateursPage() {
  const { user, loading: authLoading } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [sortOption, setSortOption] = useState<SortOption>('default');
  const [showLeaderboard, setShowLeaderboard] = useState(false);
  const { logError } = useLogger();

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const snapshot = await getDocs(collection(db, 'users'));
      const users = snapshot.docs
        .filter((doc) => isValidUser(doc.data()))
        .map((doc) => parseUser(doc.id, doc.data()));

      setUsers(users);
      setError(null);
    } catch (err) {
      logError('Failed to fetch users', {
        error: err instanceof Error ? err.message : String(err),
      });
      const errorMessage =
        err instanceof Error ? err.message : 'Erreur lors de la récupération des utilisateurs';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [logError]);

  useEffect(() => {
    if (authLoading) return;

    if (!user) {
      setError('Vous devez être connecté pour voir cette page');
      logError('Unauthorized access to users page');
      setLoading(false);
      return;
    }

    fetchUsers();
  }, [user, authLoading, fetchUsers, logError]);

  const filteredUsers = useMemo(() => {
    const filtered = users.filter((u) => {
      const matchesSearch =
        u.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.email.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesRole = roleFilter === 'ALL' || u.role === roleFilter;
      return matchesSearch && matchesRole;
    });

    if (sortOption === 'rating-desc') {
      filtered.sort((a, b) => b.rating - a.rating);
    } else if (sortOption === 'rating-asc') {
      filtered.sort((a, b) => a.rating - b.rating);
    }

    return filtered;
  }, [users, searchQuery, roleFilter, sortOption]);

  const handleUserDeleted = (userId: string) => {
    setUsers((prevUsers) => prevUsers.filter((u) => u.id !== userId));
  };

  if (authLoading || loading) {
    return (
      <div className="p-6 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <Skeleton className="h-8 w-64 mb-2" />
            <Skeleton className="h-4 w-96" />
          </div>
          <Skeleton className="h-10 w-40" />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Card key={i}>
              <CardContent className="p-6">
                <Skeleton className="h-4 w-24 mb-2" />
                <Skeleton className="h-8 w-16" />
              </CardContent>
            </Card>
          ))}
        </div>

        <Card>
          <CardContent className="p-6">
            <div className="space-y-4">
              {[1, 2, 3, 4, 5].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <Card className="border-destructive">
          <CardContent className="p-6">
            <div className="flex items-center gap-3">
              <AlertCircle className="h-5 w-5 text-destructive" />
              <div>
                <p className="font-semibold text-destructive">Erreur</p>
                <p className="text-sm text-muted-foreground">{error}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Gestion des utilisateurs</h1>
          <p className="text-muted-foreground">
            {filteredUsers.length} user{filteredUsers.length > 1 ? 's' : ''}
            {searchQuery || roleFilter !== 'ALL' ? ' trouvé(s)' : ' au total'}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={fetchUsers} disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Actualiser
          </Button>
          <Button size="sm" onClick={() => setCreateDialogOpen(true)}>
            <UserPlus className="h-4 w-4 mr-2" />
            Ajouter
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <InfoSection users={users} />

      {/* Filtres */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col gap-4">
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Rechercher par nom ou email..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9"
                />
              </div>
              <div className="flex gap-2">
                {['ALL', 'ADMIN', 'FIGHTER', 'CLIENT'].map((role) => (
                  <Button
                    key={role}
                    variant={roleFilter === role ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => setRoleFilter(role)}
                  >
                    {role === 'ALL' ? 'Tous' : role}
                  </Button>
                ))}
              </div>
            </div>
            <div className="flex flex-col sm:flex-row sm:items-center gap-4">
              <div className="flex items-center gap-2">
                <ArrowUpDown className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm text-muted-foreground">Trier par :</span>
                <div className="flex gap-2">
                  {([
                    { value: 'default', label: 'Par défaut' },
                    { value: 'rating-desc', label: 'Meilleur rating' },
                    { value: 'rating-asc', label: 'Pire rating' },
                  ] as const).map((option) => (
                    <Button
                      key={option.value}
                      variant={sortOption === option.value ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => setSortOption(option.value)}
                    >
                      {option.label}
                    </Button>
                  ))}
                </div>
              </div>
              <Button
                variant={showLeaderboard ? 'default' : 'outline'}
                size="sm"
                onClick={() => setShowLeaderboard(!showLeaderboard)}
                className="sm:ml-auto"
              >
                <Trophy className="h-4 w-4 mr-2" />
                Leaderboard
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Leaderboard */}
      {showLeaderboard && <Leaderboard users={users} />}

      {/* Table */}
      <UsersTable users={filteredUsers} onUserDeleted={handleUserDeleted} />

      {/* Dialog de création */}
      <CreateUserDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        onUserCreated={fetchUsers}
      />
    </div>
  );
}
