'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { User } from '@/types/user';
import { Users, Shield, Swords, UserCheck } from 'lucide-react';

export interface IInfoSectionProps {
  users: User[];
}

interface StatCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  description?: string;
  className?: string;
}

function StatCard({ title, value, icon, description, className }: StatCardProps) {
  return (
    <Card className={className}>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <div className="text-muted-foreground">{icon}</div>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {description && <p className="text-xs text-muted-foreground mt-1">{description}</p>}
      </CardContent>
    </Card>
  );
}

export default function InfoSection({ users }: IInfoSectionProps) {
  const totalUsers = users.length;
  const admins = users.filter((u) => u.role === 'ADMIN').length;
  const fighters = users.filter((u) => u.role === 'FIGHTER').length;
  const clients = users.filter((u) => u.role === 'CLIENT').length;
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <StatCard
        title="Total Utilisateurs"
        value={totalUsers}
        icon={<Users className="h-5 w-5" />}
        description="Tous les comptes"
      />
      <StatCard
        title="Administrateurs"
        value={admins}
        icon={<Shield className="h-5 w-5" />}
        description="Accès complet"
      />
      <StatCard
        title="Bagarreurs"
        value={fighters}
        icon={<Swords className="h-5 w-5" />}
        description="Prestataires actifs"
      />
      <StatCard
        title="Clients"
        value={clients}
        icon={<UserCheck className="h-5 w-5" />}
        description="Utilisateurs finaux"
      />
    </div>
  );
}
