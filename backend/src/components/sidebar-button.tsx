import Link from "next/link";

export interface ISidebarProps {
    href: string;
    label: string;
}

export default function SidebarButton({href, label}: ISidebarProps) {
    return (
        <Link
            href={href}
            className="block px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
        >
            🏠 {label}
        </Link>
    )
}