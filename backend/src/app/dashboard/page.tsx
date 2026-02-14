"use client";

import { useEffect, useState, useCallback } from "react";
import { Users, Swords, Star, TrendingUp } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis } from "recharts";
import type {
  DashboardStats,
  UserChartData,
  FightChartData,
  FightStatus,
} from "@/types/dashboard";
import { FIGHT_STATUS_LABELS, CHART_COLORS } from "@/types/dashboard";
import { useAuth } from "@/components/providers/auth-provider";
import useLogger from "@/hooks/useLogger";

const userChartConfig: ChartConfig = {
  clients: {
    label: "Clients",
    color: CHART_COLORS.client,
  },
  fighters: {
    label: "Bagarreurs",
    color: CHART_COLORS.fighter,
  },
} satisfies ChartConfig;

const fightChartConfig: ChartConfig = {
  completed: {
    label: "Terminés",
    color: CHART_COLORS.completed,
  },
  cancelled: {
    label: "Annulés",
    color: CHART_COLORS.cancelled,
  },
  pending: {
    label: "En attente",
    color: CHART_COLORS.pending,
  },
  inProgress: {
    label: "En cours",
    color: CHART_COLORS.inProgress,
  },
} satisfies ChartConfig;

interface KPICardProps {
  title: string;
  value: string | number;
  description?: string;
  icon: React.ReactNode;
  trend?: string;
}

function KPICard({ title, value, description, icon, trend }: KPICardProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        <div className="text-muted-foreground">{icon}</div>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {description && (
          <p className="text-xs text-muted-foreground">{description}</p>
        )}
        {trend && (
          <div className="flex items-center gap-1 text-xs text-green-600 mt-1">
            <TrendingUp className="h-3 w-3" />
            {trend}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function KPICardSkeleton() {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-5 w-5 rounded" />
      </CardHeader>
      <CardContent>
        <Skeleton className="h-8 w-16 mb-1" />
        <Skeleton className="h-3 w-32" />
      </CardContent>
    </Card>
  );
}

interface UserDistributionChartProps {
  data: UserChartData[];
}

function UserDistributionChart({ data }: UserDistributionChartProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Répartition des Utilisateurs</CardTitle>
        <CardDescription>Clients vs Bagarreurs</CardDescription>
      </CardHeader>
      <CardContent>
        <ChartContainer config={userChartConfig} className="mx-auto aspect-square max-h-64">
          <PieChart>
            <ChartTooltip
              cursor={false}
              content={<ChartTooltipContent hideLabel />}
            />
            <Pie
              data={data}
              dataKey="value"
              nameKey="name"
              innerRadius={60}
              strokeWidth={5}
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.fill} />
              ))}
            </Pie>
          </PieChart>
        </ChartContainer>
        <div className="flex justify-center gap-4 mt-4">
          {data.map((item) => (
            <div key={item.name} className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: item.fill }}
              />
              <span className="text-sm text-muted-foreground">
                {userChartConfig[item.name]?.label}: {item.value}
              </span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

function ChartSkeleton() {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-5 w-48" />
        <Skeleton className="h-4 w-32" />
      </CardHeader>
      <CardContent className="flex items-center justify-center">
        <Skeleton className="h-64 w-64 rounded-full" />
      </CardContent>
    </Card>
  );
}

interface FightStatusChartProps {
  data: FightChartData[];
}

function FightStatusChart({ data }: FightStatusChartProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>État des Combats</CardTitle>
        <CardDescription>Distribution par statut</CardDescription>
      </CardHeader>
      <CardContent>
        <ChartContainer config={fightChartConfig} className="mx-auto aspect-video max-h-64">
          <BarChart data={data} layout="vertical">
            <XAxis type="number" />
            <YAxis dataKey="status" type="category" width={80} />
            <ChartTooltip
              cursor={false}
              content={<ChartTooltipContent />}
            />
            <Bar dataKey="count" radius={[0, 4, 4, 0]}>
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.fill} />
              ))}
            </Bar>
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  );
}

function BarChartSkeleton() {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-4 w-32" />
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="flex items-center gap-2">
              <Skeleton className="h-4 w-16" />
              <Skeleton className="h-8 flex-1" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

interface RecentFightsTableProps {
  fights: DashboardStats["recentFights"];
}

function getStatusBadgeVariant(status: FightStatus): "default" | "secondary" | "destructive" | "outline" {
  switch (status) {
    case "COMPLETED":
      return "default";
    case "CANCELLED":
      return "destructive";
    case "IN_PROGRESS":
      return "secondary";
    case "PENDING":
    default:
      return "outline";
  }
}

function RecentFightsTable({ fights }: RecentFightsTableProps) {
  return (
    <Card className="col-span-full">
      <CardHeader>
        <CardTitle>Combats Récents</CardTitle>
        <CardDescription>Les 5 derniers combats sur la plateforme</CardDescription>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Client</TableHead>
              <TableHead>Bagarreur</TableHead>
              <TableHead>Lieu</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Statut</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {fights.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground">
                  Aucun combat enregistré
                </TableCell>
              </TableRow>
            ) : (
              fights.map((fight) => (
                <TableRow key={fight.id}>
                  <TableCell className="font-medium">{fight.clientName}</TableCell>
                  <TableCell>{fight.fighterName}</TableCell>
                  <TableCell>{fight.location || "Non spécifié"}</TableCell>
                  <TableCell>
                    {new Date(fight.createdAt).toLocaleDateString("fr-FR", {
                      day: "2-digit",
                      month: "short",
                      year: "numeric",
                    })}
                  </TableCell>
                  <TableCell>
                    <Badge variant={getStatusBadgeVariant(fight.status)}>
                      {FIGHT_STATUS_LABELS[fight.status]}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}

function TableSkeleton() {
  return (
    <Card className="col-span-full">
      <CardHeader>
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-4 w-56" />
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <div className="flex gap-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <Skeleton key={i} className="h-4 flex-1" />
            ))}
          </div>
          {[1, 2, 3, 4, 5].map((row) => (
            <div key={row} className="flex gap-4">
              {[1, 2, 3, 4, 5].map((cell) => (
                <Skeleton key={cell} className="h-8 flex-1" />
              ))}
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

function useDashboardStats() {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { logError } = useLogger();

  const fetchStats = useCallback(async () => {
    if (!user) {
      setError("Vous devez être connecté pour accéder au dashboard");
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      const idToken = await user.getIdToken();

      const response = await fetch("/api/dashboard/stats", {
        headers: {
          Authorization: `Bearer ${idToken}`,
        },
      });

      if (response.status === 401) {
        throw new Error("Session expirée - Veuillez vous reconnecter");
      }

      if (response.status === 403) {
        throw new Error("Accès non autorisé - Droits administrateur requis");
      }

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || `Erreur HTTP: ${response.status}`);
      }

      const stats: DashboardStats = await response.json();
      setData(stats);
    } catch (err) {
      logError("Failed to fetch dashboard stats", { error: err instanceof Error ? err.message : String(err) });
      setError(err instanceof Error ? err.message : "Erreur inconnue");
    } finally {
      setIsLoading(false);
    }
  }, [user, logError]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  return { data, isLoading, error };
}

function transformUserData(distribution: DashboardStats["userDistribution"]): UserChartData[] {
  return [
    {
      name: "clients",
      value: distribution.clients,
      fill: CHART_COLORS.client,
    },
    {
      name: "fighters",
      value: distribution.fighters,
      fill: CHART_COLORS.fighter,
    },
  ];
}

function transformFightData(distribution: DashboardStats["fightStatusDistribution"]): FightChartData[] {
  return [
    {
      status: "Terminés",
      count: distribution.completed,
      fill: CHART_COLORS.completed,
    },
    {
      status: "Annulés",
      count: distribution.cancelled,
      fill: CHART_COLORS.cancelled,
    },
    {
      status: "En attente",
      count: distribution.pending,
      fill: CHART_COLORS.pending,
    },
    {
      status: "En cours",
      count: distribution.inProgress,
      fill: CHART_COLORS.inProgress,
    },
  ];
}

export default function DashboardPage() {
  const { data, isLoading, error } = useDashboardStats();

  if (error) {
    return (
      <div className="p-6">
        <div className="flex items-center justify-center min-h-96">
          <Card className="p-6">
            <CardContent className="text-center">
              <p className="text-destructive font-medium">Erreur de chargement</p>
              <p className="text-muted-foreground text-sm mt-2">{error}</p>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="p-6">
        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
            <p className="text-muted-foreground">
              Vue d&apos;ensemble de la plateforme Uber Fight
            </p>
          </div>

          {/* KPI Cards Skeleton */}
          <div className="grid gap-4 md:grid-cols-3">
            <KPICardSkeleton />
            <KPICardSkeleton />
            <KPICardSkeleton />
          </div>

          {/* Charts Skeleton */}
          <div className="grid gap-4 md:grid-cols-2">
            <ChartSkeleton />
            <BarChartSkeleton />
          </div>

          {/* Table Skeleton */}
          <TableSkeleton />
        </div>
      </div>
    );
  }

  const userChartData = data ? transformUserData(data.userDistribution) : [];
  const fightChartData = data ? transformFightData(data.fightStatusDistribution) : [];

  return (
    <div className="p-6">
      <div className="space-y-6">
        {/* En-tête */}
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground">
            Vue d&apos;ensemble de la plateforme Uber Fight
          </p>
        </div>

        {/* KPI Cards */}
        <div className="grid gap-4 md:grid-cols-3">
          <KPICard
            title="Total Utilisateurs"
            value={data?.userDistribution.total ?? 0}
            description={`${data?.userDistribution.clients ?? 0} clients, ${data?.userDistribution.fighters ?? 0} bagarreurs`}
            icon={<Users className="h-5 w-5" />}
          />
          <KPICard
            title="Total Combats"
            value={data?.fightStatusDistribution.total ?? 0}
            description={`${data?.fightStatusDistribution.completed ?? 0} terminés`}
            icon={<Swords className="h-5 w-5" />}
          />
          <KPICard
            title="Note Moyenne"
            value={data?.averageRating ? `${data.averageRating}/5` : "N/A"}
            description="Note moyenne des bagarreurs"
            icon={<Star className="h-5 w-5" />}
          />
        </div>

        {/* Graphiques */}
        <div className="grid gap-4 md:grid-cols-2">
          <UserDistributionChart data={userChartData} />
          <FightStatusChart data={fightChartData} />
        </div>

        {/* Table des combats récents */}
        {data && <RecentFightsTable fights={data.recentFights} />}
      </div>
    </div>
  );
}
