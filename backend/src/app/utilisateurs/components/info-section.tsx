'use client';

import Card from "@/components/card";

export interface IInfoSectionProps {
    utilisateurs: {
        id: number;
        nom: string;
        prenom: string;
        email: string;
        role: string;
        dateInscription: string;
    }[];
}

export default function InfoSection({utilisateurs}: IInfoSectionProps) {
    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <Card
                title="Total des utilisateurs">
                {utilisateurs.length}
            </Card>
            <Card title={'Administrateurs'}
            >
                {utilisateurs.filter(u => u.role === "Administrateur").length}
            </Card>
            <Card title={'Utilisateurs actifs'}>
                {utilisateurs.filter(u => u.role === "Utilisateur").length}
            </Card>
        </div>
    )
}