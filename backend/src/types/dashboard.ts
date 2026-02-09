export type UserRole = "CLIENT" | "FIGHTER";

export type FightStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface UserDistribution {
  clients: number;
  fighters: number;
  total: number;
}

export interface FightStatusDistribution {
  pending: number;
  inProgress: number;
  completed: number;
  cancelled: number;
  total: number;
}

export interface RecentFight {
  id: string;
  clientName: string;
  fighterName: string;
  status: FightStatus;
  createdAt: string;
  location?: string;
}

export interface DashboardStats {
  userDistribution: UserDistribution;
  fightStatusDistribution: FightStatusDistribution;
  averageRating: number;
  recentFights: RecentFight[];
}

export interface UserChartData {
  name: string;
  value: number;
  fill: string;
}

export interface FightChartData {
  status: string;
  count: number;
  fill: string;
}

export const FIGHT_STATUS_LABELS: Record<FightStatus, string> = {
  PENDING: "En attente",
  IN_PROGRESS: "En cours",
  COMPLETED: "Terminé",
  CANCELLED: "Annulé",
};

export const CHART_COLORS = {
  client: "hsl(221, 83%, 53%)", // Bleu
  fighter: "hsl(0, 84%, 60%)", // Rouge
  completed: "hsl(142, 71%, 45%)", // Vert
  cancelled: "hsl(0, 84%, 60%)", // Rouge
  pending: "hsl(38, 92%, 50%)", // Orange
  inProgress: "hsl(221, 83%, 53%)", // Bleu
} as const;
