import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import { pushGauge } from "./lib/grafana-metrics";
import { logFunction } from "./lib/grafana-logger";

const REGION = "europe-west1";

export const collectBusinessMetrics = onSchedule(
  {
    schedule: "every 5 minutes",
    region: REGION,
    timeoutSeconds: 60,
  },
  async () => {
    try {
      const db = admin.firestore();

      const usersSnapshot = await db.collection("users").get();
      let clients = 0;
      let fighters = 0;
      let admins = 0;
      let totalRating = 0;
      let ratedFighters = 0;

      usersSnapshot.docs.forEach((doc) => {
        const data = doc.data();
        const role = (data.role || "CLIENT").toUpperCase();

        if (role === "FIGHTER") {
          fighters++;
          if (typeof data.rating === "number" && data.rating > 0) {
            totalRating += data.rating;
            ratedFighters++;
          }
        } else if (role === "ADMIN") {
          admins++;
        } else {
          clients++;
        }
      });

      await pushGauge("business.users.total", usersSnapshot.size);
      await pushGauge("business.users.by_role", clients, { role: "client" });
      await pushGauge("business.users.by_role", fighters, { role: "fighter" });
      await pushGauge("business.users.by_role", admins, { role: "admin" });

      const averageRating = ratedFighters > 0
        ? Math.round((totalRating / ratedFighters) * 10) / 10
        : 0;
      await pushGauge("business.average_rating", averageRating);

      const fightsSnapshot = await db.collection("fights").get();
      const statuses: Record<string, number> = {
        pending: 0,
        in_progress: 0,
        completed: 0,
        cancelled: 0,
      };

      fightsSnapshot.docs.forEach((doc) => {
        const data = doc.data();
        const status = (data.status || "PENDING").toUpperCase();

        switch (status) {
          case "PENDING":
            statuses.pending++;
            break;
          case "IN_PROGRESS":
            statuses.in_progress++;
            break;
          case "COMPLETED":
            statuses.completed++;
            break;
          case "CANCELLED":
            statuses.cancelled++;
            break;
        }
      });

      const totalFights = fightsSnapshot.size;
      await pushGauge("business.fights.total", totalFights);

      for (const [status, count] of Object.entries(statuses)) {
        await pushGauge("business.fights.by_status", count, { status });
      }

      const completionRate = totalFights > 0
        ? Math.round((statuses.completed / totalFights) * 1000) / 10
        : 0;
      await pushGauge("business.fights.completion_rate", completionRate);

      await logFunction("collectBusinessMetrics", "Business metrics collected", "info", {
        totalUsers: usersSnapshot.size,
        totalFights,
        completionRate,
        averageRating,
      });
    } catch (error) {
      await logFunction("collectBusinessMetrics", "Failed to collect business metrics", "error", {
        errorMessage: error instanceof Error ? error.message : String(error),
      });
    }
  }
);
