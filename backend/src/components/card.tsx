'use client';

import React from 'react';

export interface ICardProps {
  title: string;
  children: React.ReactNode;
}

export default function Card({ title, children }: ICardProps) {
  return (
    <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
      <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">{title}</div>
      <div className="text-3xl font-bold text-gray-900 dark:text-white">{children}</div>
    </div>
  );
}
