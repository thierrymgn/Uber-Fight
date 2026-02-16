import { Badge } from '@/components/ui/badge';
import { Shield, Swords, User } from 'lucide-react';

export interface UserRoleTagProps {
  role: string;
}

const roleConfig: Record<
  string,
  {
    label: string;
    variant: 'default' | 'secondary' | 'destructive' | 'outline';
    icon: React.ReactNode;
  }
> = {
  ADMIN: {
    label: 'Admin',
    variant: 'default',
    icon: <Shield className="h-3 w-3" />,
  },
  FIGHTER: {
    label: 'Bagarreur',
    variant: 'destructive',
    icon: <Swords className="h-3 w-3" />,
  },
  CLIENT: {
    label: 'Client',
    variant: 'secondary',
    icon: <User className="h-3 w-3" />,
  },
};

export default function UserRoleTag({ role }: UserRoleTagProps) {
  const config = roleConfig[role] || {
    label: role,
    variant: 'outline' as const,
    icon: <User className="h-3 w-3" />,
  };

  return (
    <Badge variant={config.variant} className="gap-1">
      {config.icon}
      {config.label}
    </Badge>
  );
}
