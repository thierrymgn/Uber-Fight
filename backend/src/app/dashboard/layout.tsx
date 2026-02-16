import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Dashboard | Uber Fight Admin',
  description: 'Tableau de bord administrateur de la plateforme Uber Fight',
};

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
