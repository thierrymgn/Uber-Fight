'use client';

import { useEffect } from 'react';
import { useAuth } from '@/components/providers/auth-provider';
import { usePathname, useRouter } from 'next/navigation';
import Sidebar from '@/components/sidebar';

export function LayoutContent({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  const publicPages = ['/login'];
  const isPublicPage = publicPages.includes(pathname);

  const showSidebar = user && !isPublicPage;

  useEffect(() => {
    if (!loading && !user && !isPublicPage) {
      const redirectPath = encodeURIComponent(pathname || '/');
      router.push(`/login?redirect=${redirectPath}`);
    }
  }, [loading, user, isPublicPage, pathname, router]);

  if (loading || (!user && !isPublicPage)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 dark:bg-gray-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Chargement...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-100 dark:bg-gray-900">
      {showSidebar && <Sidebar />}
      <main className={showSidebar ? 'flex-1' : 'w-full'}>{children}</main>
    </div>
  );
}
