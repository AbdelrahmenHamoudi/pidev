package org.example.jobs;

import org.example.services.NotificationService;
import org.quartz.*;

/**
 * Job Quartz — Alerte 1h avant expiration de la promo.
 */
public class PromoWarning1hJob implements Job {
    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap data  = ctx.getJobDetail().getJobDataMap();
        String promoName = data.getString("promoName");
        String endDate   = data.getString("endDate");

        System.out.println("[Quartz] 🚨 ALERTE 1H — Expiration imminente : " + promoName);

        NotificationService.getInstance().showModal(
                NotificationService.Type.DANGER,
                "🚨  Expire dans 1 heure !",
                "\"" + promoName + "\" expire dans moins d'une heure !\n\n" +
                        "Date de fin : " + endDate + "\n\n" +
                        "Dernière chance pour les clients de réserver."
        );

        NotificationService.getInstance().danger(
                "Expiration imminente !",
                "🚨 " + promoName + " — expire dans < 1h !"
        );
    }
}
