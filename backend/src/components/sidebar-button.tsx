import Link from "next/link";
import { ReactNode } from "react";

export interface ISidebarProps {
    href: string;
    label: string;
    icon: ReactNode;
}

export default function SidebarButton({href, label, icon}: ISidebarProps) {
    return (
        <Link
            href={href}
            className="flex items-center gap-3 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
        >
            {icon && <span className="flex-shrink-0">{icon}</span>}
            <span>{label}</span>
        </Link>
    )
}