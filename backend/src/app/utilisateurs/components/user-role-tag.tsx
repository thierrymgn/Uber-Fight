export interface UserRoleTagProps {
    role: string;
}

export default function UserRoleTag(
    {role}: UserRoleTagProps
) {
    return (
        <span
            className={`px-2 py-1 rounded-full text-xs font-medium ${
                role === "Fighter"
                    ? "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200"
                    : role === "FIGHTER"
                        ? "bg-blue-100 text-red-900 dark:bg-red-900 dark:text-blue-200"
                        : "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
            }`}
        >
                        {role}
        </span>
    )
}