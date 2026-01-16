import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Connexion - Uber Fight",
  description: "Connexion à l'application Uber Fight",
};

export default function LoginLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <>{children}</>;
}

