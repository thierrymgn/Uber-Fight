import { getAdminFirestore, getAdminAuth } from "@/lib/firebase-admin";

export interface AuthUser {
  uid: string;
  email: string | undefined;
  role: string;
}

export type AuthResult =
  | {
      success: true;
      user: AuthUser;
    }
  | {
      success: false;
      error: string;
      status: 401 | 403;
    };

export async function verifyAuth(
  request: Request,
  requiredRoles?: string[]
): Promise<AuthResult> {
  const authHeader = request.headers.get("Authorization");

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return {
      success: false,
      error: "Token d'authentification manquant",
      status: 401,
    };
  }

  const idToken = authHeader.split("Bearer ")[1];

  if (!idToken) {
    return {
      success: false,
      error: "Token d'authentification invalide",
      status: 401,
    };
  }

  try {
    const auth = getAdminAuth();
    const decodedToken = await auth.verifyIdToken(idToken);

    const db = getAdminFirestore();
    const userDoc = await db.collection("users").doc(decodedToken.uid).get();

    if (!userDoc.exists) {
      return {
        success: false,
        error: "Utilisateur non trouvé",
        status: 401,
      };
    }

    const userData = userDoc.data();
    const userRole = userData?.role?.toUpperCase() || "CLIENT";

    if (requiredRoles && requiredRoles.length > 0) {
      const hasRequiredRole = requiredRoles
        .map((r) => r.toUpperCase())
        .includes(userRole);

      if (!hasRequiredRole) {
        return {
          success: false,
          error: "Accès non autorisé - Rôle insuffisant",
          status: 403,
        };
      }
    }

    return {
      success: true,
      user: {
        uid: decodedToken.uid,
        email: decodedToken.email,
        role: userRole,
      },
    };
  } catch (error) {
    console.error("Erreur de vérification du token:", error);

    if (error instanceof Error) {
      if (error.message.includes("expired")) {
        return {
          success: false,
          error: "Token expiré - Veuillez vous reconnecter",
          status: 401,
        };
      }
      if (error.message.includes("invalid") || error.message.includes("malformed")) {
        return {
          success: false,
          error: "Token invalide",
          status: 401,
        };
      }
    }

    return {
      success: false,
      error: "Erreur d'authentification",
      status: 401,
    };
  }
}
