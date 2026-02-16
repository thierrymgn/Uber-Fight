'use client';

import { useMemo } from 'react';
import { User } from '@/types/user';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Trophy, Star, Medal } from 'lucide-react';

export interface LeaderboardProps {
  users: User[];
}

export default function Leaderboard({ users }: LeaderboardProps) {
  const rankedUsers = useMemo(() => {
    return users
      .filter((u) => u.role !== 'ADMIN')
      .sort((a, b) => b.rating - a.rating || b.ratingCount - a.ratingCount);
  }, [users]);

  const getUserInitials = (username: string) => {
    return username
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  const getRankIcon = (index: number) => {
    if (index === 0) return <Trophy className="h-5 w-5 text-yellow-500" />;
    if (index === 1) return <Medal className="h-5 w-5 text-gray-400" />;
    if (index === 2) return <Medal className="h-5 w-5 text-amber-700" />;
    return <span className="text-sm font-bold text-muted-foreground w-5 text-center">{index + 1}</span>;
  };

  const getRankBg = (index: number) => {
    if (index === 0) return 'bg-yellow-500/10 border-yellow-500/30';
    if (index === 1) return 'bg-gray-200/30 border-gray-300/30';
    if (index === 2) return 'bg-amber-700/10 border-amber-700/30';
    return '';
  };

  if (rankedUsers.length === 0) {
    return (
      <Card>
        <CardContent className="p-6 text-center text-muted-foreground">
          Aucun utilisateur à classer (les admins sont exclus).
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Trophy className="h-5 w-5 text-yellow-500" />
          <CardTitle>Leaderboard</CardTitle>
        </div>
        <CardDescription>
          Classement des utilisateurs par rating (admins exclus) — {rankedUsers.length} utilisateur{rankedUsers.length > 1 ? 's' : ''}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          {rankedUsers.map((user, index) => (
            <div
              key={user.id}
              className={`flex items-center gap-4 p-3 rounded-lg border ${getRankBg(index)}`}
            >
              <div className="flex items-center justify-center w-8">
                {getRankIcon(index)}
              </div>
              <Avatar className="h-9 w-9">
                <AvatarImage src={user.photoUrl || undefined} />
                <AvatarFallback className="text-xs">
                  {getUserInitials(user.username)}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <p className="font-medium truncate">{user.username}</p>
                <p className="text-xs text-muted-foreground capitalize">{user.role.toLowerCase()}</p>
              </div>
              <div className="flex items-center gap-1">
                <Star className="h-4 w-4 text-yellow-500 fill-yellow-500" />
                <span className="font-bold">{user.rating.toFixed(1)}</span>
                <span className="text-xs text-muted-foreground">({user.ratingCount} avis)</span>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
