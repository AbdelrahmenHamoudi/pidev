package org.example.Services.promotion.promotionJobs;


import org.example.Services.promotion.NotificationService;
import org.quartz.*;

/**
 * Job Quartz — Activation automatique d'une promo à startDate 00:00.
 */
public class PromoActivationJob implements Job {
    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap data  = ctx.getJobDetail().getJobDataMap();
        int    promoId   = data.getInt("promoId");
        String promoName = data.getString("promoName");
        String startDate = data.getString("startDate");

        System.out.println("[Quartz] ✅ ACTIVATION automatique : " + promoName + " (ID:" + promoId + ")");

        // Modal important centré
        NotificationService.getInstance().showModal(
                NotificationService.Type.PROMO,
                "🎁  Promotion Activée !",
                "\"" + promoName + "\" est maintenant active depuis le " + startDate + ".\n\n" +
                        "Les clients peuvent la réserver dès maintenant."
        );

        // Toast supplémentaire
        NotificationService.getInstance().success(
                "Activation automatique",
                "🎁 " + promoName + " est maintenant disponible !"
        );
    }
}
