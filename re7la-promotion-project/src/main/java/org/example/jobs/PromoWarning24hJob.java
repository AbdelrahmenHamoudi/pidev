package org.example.jobs;

import org.example.services.NotificationService;
import org.quartz.*;

/**
 * Job Quartz — Alerte 24h avant expiration de la promo.
 */
public class PromoWarning24hJob implements Job {
    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap data  = ctx.getJobDetail().getJobDataMap();
        String promoName = data.getString("promoName");
        String endDate   = data.getString("endDate");

        System.out.println("[Quartz] ⚠️ ALERTE 24H — Expiration demain : " + promoName);

        NotificationService.getInstance().showModal(
                NotificationService.Type.WARNING,
                "⚠️  Promotion expire demain !",
                "\"" + promoName + "\" expire le " + endDate + ".\n\n" +
                        "Pensez à la prolonger dans le BackOffice si nécessaire."
        );

        NotificationService.getInstance().warning(
                "Expire dans 24h",
                "⚠️ " + promoName + " expire le " + endDate
        );
    }
}
