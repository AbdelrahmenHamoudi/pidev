package org.example.Utils;

import animatefx.animation.*;
import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.List;

/**
 * Helper centralisé pour toutes les animations AnimateFX de RE7LA.
 *
 * Utilisé dans :
 *   - PromotionBackOfficeController  (cards, boutons, stats)
 *   - StatistiquesReservationsController (charts, KPI)
 *   - MainApp (animation d'entrée globale)
 */
public class AnimationHelper {

    // ═══ ENTRÉES ═══

    public static void fadeInUp(Node node) {
        node.setOpacity(0);
        FadeInUp a = new FadeInUp(node);
        a.setOnFinished(e -> node.setOpacity(1));
        a.play();
    }

    public static void zoomIn(Node node) {
        node.setOpacity(0);
        ZoomIn a = new ZoomIn(node);
        a.setOnFinished(e -> node.setOpacity(1));
        a.play();
    }

    public static void bounceIn(Node node) {
        new BounceIn(node).play();
    }

    public static void slideInRight(Node node) {
        new SlideInRight(node).play();
    }

    public static void slideInLeft(Node node) {
        new SlideInLeft(node).play();
    }

    public static void slideInDown(Node node) {
        new SlideInDown(node).play();
    }

    public static void fadeIn(Node node) {
        node.setOpacity(0);
        FadeIn a = new FadeIn(node);
        a.setOnFinished(e -> node.setOpacity(1));
        a.play();
    }

    // ═══ ATTENTIONS ═══

    public static void flash(Node node)     { new Flash(node).play(); }
    public static void pulse(Node node)     { new Pulse(node).play(); }
    public static void shake(Node node)     { new Shake(node).play(); }
    public static void tada(Node node)      { new Tada(node).play(); }
    public static void rubberBand(Node node){ new RubberBand(node).play(); }
    public static void swing(Node node)     { new Swing(node).play(); }

    // ═══ SORTIES ═══

    public static void fadeOut(Node node, Runnable onFinished) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> onFinished.run());
        ft.play();
    }

    public static void zoomOut(Node node, Runnable onFinished) {
        ZoomOut a = new ZoomOut(node);
        a.setOnFinished(e -> onFinished.run());
        a.play();
    }

    // ═══ ANIMATIONS EN CASCADE ═══

    /**
     * Anime une liste de nodes en FadeInUp avec délai croissant.
     * Parfait pour les cards promos.
     * @param nodes   nodes à animer
     * @param delayMs délai entre chaque (ex: 80)
     */
    public static void staggeredFadeIn(List<? extends Node> nodes, int delayMs) {
        for (int i = 0; i < nodes.size(); i++) {
            final Node node = nodes.get(i);
            final int index = i;
            node.setOpacity(0);
            PauseTransition pause = new PauseTransition(Duration.millis((long) delayMs * index));
            pause.setOnFinished(e -> {
                node.setOpacity(1);
                new FadeInUp(node).play();
            });
            pause.play();
        }
    }

    /**
     * ZoomIn en cascade — pour les graphiques stats.
     */
    public static void staggeredZoomIn(List<? extends Node> nodes, int delayMs) {
        for (int i = 0; i < nodes.size(); i++) {
            final Node node = nodes.get(i);
            final int index = i;
            node.setOpacity(0);
            PauseTransition pause = new PauseTransition(Duration.millis((long) delayMs * index));
            pause.setOnFinished(e -> {
                node.setOpacity(1);
                new ZoomIn(node).play();
            });
            pause.play();
        }
    }

    /**
     * BounceIn en cascade — pour les KPI stat cards.
     */
    public static void staggeredBounceIn(List<? extends Node> nodes, int delayMs) {
        for (int i = 0; i < nodes.size(); i++) {
            final Node node = nodes.get(i);
            final int index = i;
            node.setOpacity(0);
            PauseTransition pause = new PauseTransition(Duration.millis((long) delayMs * index));
            pause.setOnFinished(e -> {
                node.setOpacity(1);
                new BounceIn(node).play();
            });
            pause.play();
        }
    }

    // ═══ COUNT-UP ANIMÉ ═══

    /**
     * Anime un Label qui compte de 0 → valeur en durationMs ms.
     * Parfait pour les KPI du dashboard.
     */
    public static void countUp(Label label, int startVal, int endVal, int durationMs) {
        if (endVal <= 0) { label.setText("0"); return; }
        int frames   = 40;
        int frameMs  = Math.max(1, durationMs / frames);
        Timeline tl  = new Timeline();
        for (int i = 0; i <= frames; i++) {
            final int val = startVal + (int) ((endVal - startVal) * easeOut(i / (double) frames));
            tl.getKeyFrames().add(new KeyFrame(Duration.millis((long) frameMs * i),
                    e -> label.setText(String.valueOf(val))));
        }
        tl.getKeyFrames().add(new KeyFrame(Duration.millis((long) frameMs * frames + 1),
                e -> label.setText(String.valueOf(endVal))));
        tl.play();
    }

    /**
     * Count-up avec suffixe.
     */
    public static void countUp(Label label, int startVal, int endVal, int durationMs, String suffix) {
        if (endVal <= 0) { label.setText("0" + suffix); return; }
        int frames  = 40;
        int frameMs = Math.max(1, durationMs / frames);
        Timeline tl = new Timeline();
        for (int i = 0; i <= frames; i++) {
            final int val = startVal + (int) ((endVal - startVal) * easeOut(i / (double) frames));
            tl.getKeyFrames().add(new KeyFrame(Duration.millis((long) frameMs * i),
                    e -> label.setText(val + suffix)));
        }
        tl.getKeyFrames().add(new KeyFrame(Duration.millis((long) frameMs * frames + 1),
                e -> label.setText(endVal + suffix)));
        tl.play();
    }

    /** Easing cubique pour le countUp */
    private static double easeOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    // ═══ HOVER EFFECTS ═══

    public static void addPulseOnHover(Node node) {
        node.setOnMouseEntered(e -> new Pulse(node).play());
    }

    public static void addFlashOnHover(Node node) {
        node.setOnMouseEntered(e -> new Flash(node).play());
    }
}