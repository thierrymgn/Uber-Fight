"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/components/providers/auth-provider";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  Users,
  Home,
  LogOut,
  ChevronLeft,
  Swords,
} from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useState } from "react";

const navigationItems = [
  {
    title: "Accueil",
    href: "/",
    icon: Home,
  },
  {
    title: "Dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    title: "Utilisateurs",
    href: "/utilisateurs",
    icon: Users,
  },
];

interface NavItemProps {
  href: string;
  icon: React.ElementType;
  title: string;
  isActive: boolean;
  isCollapsed: boolean;
}

function NavItem({ href, icon: Icon, title, isActive, isCollapsed }: NavItemProps) {
  const content = (
    <Link
      href={href}
      className={cn(
        "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200",
        "hover:bg-accent hover:text-accent-foreground",
        isActive
          ? "bg-primary text-primary-foreground shadow-sm"
          : "text-muted-foreground",
        isCollapsed && "justify-center px-2"
      )}
    >
      <Icon className={cn("h-5 w-5 shrink-0", isActive && "text-primary-foreground")} />
      {!isCollapsed && <span>{title}</span>}
    </Link>
  );

  if (isCollapsed) {
    return (
      <Tooltip delayDuration={0}>
        <TooltipTrigger asChild>{content}</TooltipTrigger>
        <TooltipContent side="right" className="flex items-center gap-4">
          {title}
        </TooltipContent>
      </Tooltip>
    );
  }

  return content;
}

export default function Sidebar() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [isCollapsed, setIsCollapsed] = useState(false);

  const handleLogout = async () => {
    try {
      await logout();
      router.push("/login");
    } catch (error) {
      console.error("Erreur lors de la déconnexion:", error);
    }
  };

  const getUserInitials = (email: string) => {
    return email.substring(0, 2).toUpperCase();
  };

  return (
    <TooltipProvider>
      <aside
        className={cn(
          "relative flex h-screen flex-col border-r bg-card transition-all duration-300",
          isCollapsed ? "w-16" : "w-64"
        )}
      >
        {/* Bouton collapse */}
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className={cn(
            "absolute -right-3 top-6 z-10 flex h-6 w-6 items-center justify-center rounded-full border bg-background shadow-sm transition-transform hover:bg-accent",
            isCollapsed && "rotate-180"
          )}
        >
          <ChevronLeft className="h-4 w-4" />
        </button>

        {/* Logo */}
        <div className={cn(
          "flex shrink-0 items-center border-b p-4",
          isCollapsed && "justify-center p-3"
        )}>
          {isCollapsed ? (
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary shrink-0">
              <Swords className="h-6 w-6 text-primary-foreground" />
            </div>
          ) : (
            <Image
              src="/logo/ic_logo_uber_fight.png"
              alt="Uber Fight"
              width={200}
              height={80}
              className="w-full h-auto object-contain"
              priority
            />
          )}
        </div>

        {/* Navigation principale */}
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {navigationItems.map((item) => (
            <NavItem
              key={item.href}
              href={item.href}
              icon={item.icon}
              title={item.title}
              isActive={pathname === item.href}
              isCollapsed={isCollapsed}
            />
          ))}
        </nav>

        <Separator />

        {/* Section utilisateur */}
        <div className={cn("p-3", isCollapsed && "flex justify-center")}>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                className={cn(
                  "flex w-full items-center gap-3 rounded-lg p-2 text-left transition-colors hover:bg-accent",
                  isCollapsed && "justify-center"
                )}
              >
                <Avatar className="h-9 w-9 shrink-0">
                  <AvatarImage src={user?.photoURL || undefined} />
                  <AvatarFallback className="bg-primary text-primary-foreground text-xs">
                    {user?.email ? getUserInitials(user.email) : "AD"}
                  </AvatarFallback>
                </Avatar>
                {!isCollapsed && (
                  <div className="flex-1 overflow-hidden">
                    <p className="truncate text-sm font-medium">
                      {user?.displayName || "Administrateur"}
                    </p>
                    <p className="truncate text-xs text-muted-foreground">
                      {user?.email}
                    </p>
                  </div>
                )}
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              align={isCollapsed ? "center" : "end"}
              side="top"
              className="w-56"
            >
              <DropdownMenuLabel>
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium">
                    {user?.displayName || "Administrateur"}
                  </p>
                  <p className="text-xs text-muted-foreground">{user?.email}</p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={handleLogout}
                className="cursor-pointer text-destructive focus:bg-destructive/10 focus:text-destructive"
              >
                <LogOut className="mr-2 h-4 w-4" />
                Déconnexion
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </aside>
    </TooltipProvider>
  );
}