package org.example.Services.promotion;

import org.example.Entites.promotion.Promotion;                          // ✅
import org.example.Services.promotion.promotionJobs.PromoActivationJob; // ✅
import org.example.Services.promotion.promotionJobs.PromoWarning1hJob;  // ✅
import org.example.Services.promotion.promotionJobs.PromoWarning24hJob; // ✅
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * SchedulerService — Singleton Quartz (RAM uniquement).
 *
 * Jobs planifiés pour chaque promotion :
 *  ① PromoActivationJob   → startDate à 00:00
 *  ② PromoWarning24hJob   → endDate - 24h
 *  ③ PromoWarning1hJob    → endDate - 1h
 *
 * Démarrage dans MainApp.start(), arrêt dans onCloseRequest.
 */
public class SchedulerService {

    private static SchedulerService instance;
    private Scheduler scheduler;

    private SchedulerService() {}

    public static SchedulerService getInstance() {
        if (instance == null) instance = new SchedulerService();
        return instance;
    }

    // ═══════════════════════════════════════════════════
    // DÉMARRAGE
    // ═══════════════════════════════════════════════════

    public void start() {
        try {
            Properties props = new Properties();
            props.setProperty("org.quartz.scheduler.instanceName",    "RE7LAScheduler");
            props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
            props.setProperty("org.quartz.jobStore.class",            "org.quartz.simpl.RAMJobStore");
            props.setProperty("org.quartz.threadPool.class",          "org.quartz.simpl.SimpleThreadPool");
            props.setProperty("org.quartz.threadPool.threadCount",    "5");
            props.setProperty("org.quartz.threadPool.threadPriority", "5");

            scheduler = new StdSchedulerFactory(props).getScheduler();
            scheduler.start();
            System.out.println("[Quartz] ✅ Scheduler RE7LA démarré (RAM)");

            // Planifier toutes les promos existantes
            scheduleAllExistingPromos();

        } catch (SchedulerException e) {
            System.err.println("[Quartz] ❌ Erreur démarrage : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    // ARRÊT PROPRE
    // ═══════════════════════════════════════════════════

    public void stop() {
        try {
            if (scheduler != null && scheduler.isStarted()) {
                scheduler.shutdown(true);
                System.out.println("[Quartz] 🛑 Scheduler arrêté proprement.");
            }
        } catch (SchedulerException e) {
            System.err.println("[Quartz] Erreur arrêt : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    // PLANIFIER TOUTES LES PROMOS EXISTANTES
    // ═══════════════════════════════════════════════════

    private void scheduleAllExistingPromos() {
        List<Promotion> promos = new PromotionService().getAll();
        int count = 0;
        for (Promotion p : promos) {
            if (schedulePromo(p)) count++;
        }
        System.out.println("[Quartz] 📅 " + count + " job(s) planifié(s) sur " + promos.size() + " promo(s)");
    }

    // ═══════════════════════════════════════════════════
    // PLANIFIER UNE PROMOTION (créa / modif)
    // ═══════════════════════════════════════════════════

    /**
     * Planifie les 3 jobs pour une promotion.
     * Les dates passées sont ignorées silencieusement.
     * @return true si au moins 1 job a été planifié
     */
    public boolean schedulePromo(Promotion promo) {
        if (scheduler == null) return false;
        LocalDateTime now = LocalDateTime.now();
        boolean any = false;

        // ① Activation à startDate 00:00
        LocalDateTime activationTime = promo.getStartDate().toLocalDate().atStartOfDay();
        if (activationTime.isAfter(now)) {
            doSchedule(PromoActivationJob.class,
                    "activation_" + promo.getId(), "activations",
                    toDate(activationTime), buildMap(promo));
            System.out.printf("[Quartz] ⏰ Activation planifiée : %-20s → %s%n", promo.getName(), activationTime);
            any = true;
        }

        // ② Alerte 24h avant fin
        LocalDateTime warn24 = promo.getEndDate().toLocalDate().atTime(23, 59).minusHours(24);
        if (warn24.isAfter(now)) {
            doSchedule(PromoWarning24hJob.class,
                    "warn24h_" + promo.getId(), "warnings",
                    toDate(warn24), buildMap(promo));
            System.out.printf("[Quartz] ⏰ Alerte 24h planifiée  : %-20s → %s%n", promo.getName(), warn24);
            any = true;
        }

        // ③ Alerte 1h avant fin
        LocalDateTime warn1h = promo.getEndDate().toLocalDate().atTime(23, 59).minusHours(1);
        if (warn1h.isAfter(now)) {
            doSchedule(PromoWarning1hJob.class,
                    "warn1h_" + promo.getId(), "warnings",
                    toDate(warn1h), buildMap(promo));
            System.out.printf("[Quartz] ⏰ Alerte 1h planifiée   : %-20s → %s%n", promo.getName(), warn1h);
            any = true;
        }

        return any;
    }

    /**
     * Supprime les 3 jobs d'une promo (avant suppression ou modification).
     */
    public void unschedulePromo(int promoId) {
        if (scheduler == null) return;
        try {
            scheduler.deleteJob(JobKey.jobKey("activation_" + promoId, "activations"));
            scheduler.deleteJob(JobKey.jobKey("warn24h_"    + promoId, "warnings"));
            scheduler.deleteJob(JobKey.jobKey("warn1h_"     + promoId, "warnings"));
            System.out.println("[Quartz] 🗑️  Jobs supprimés pour promo #" + promoId);
        } catch (SchedulerException e) {
            System.err.println("[Quartz] Erreur unschedule : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    // TEST — notification dans 5 secondes
    // ═══════════════════════════════════════════════════

    /**
     * Déclenche une notification test dans 5 secondes.
     * Décommenter dans MainApp pour tester.
     */
    public void scheduleTestIn5s() {
        // Test désactivé — ne plus appeler
        System.out.println("[Quartz] scheduleTestIn5s() désactivé.");
    }

    // ═══════════════════════════════════════════════════
    // HELPERS PRIVÉS
    // ═══════════════════════════════════════════════════

    private void doSchedule(Class<? extends Job> jobClass,
                            String name, String group,
                            Date when, JobDataMap data) {
        try {
            if (scheduler.checkExists(JobKey.jobKey(name, group)))
                scheduler.deleteJob(JobKey.jobKey(name, group));

            JobDetail job = JobBuilder.newJob(jobClass)
                    .withIdentity(name, group).usingJobData(data).build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("t_" + name, group)
                    .startAt(when)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            System.err.println("[Quartz] Erreur schedule " + name + " : " + e.getMessage());
        }
    }

    private JobDataMap buildMap(Promotion p) {
        JobDataMap m = new JobDataMap();
        m.put("promoId",   p.getId());
        m.put("promoName", p.getName());
        m.put("startDate", p.getStartDate().toString());
        m.put("endDate",   p.getEndDate().toString());
        return m;
    }

    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}