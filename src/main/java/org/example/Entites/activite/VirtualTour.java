package org.example.Entites.activite;

import java.util.ArrayList;
import java.util.List;

public class VirtualTour {
    private String lieu;
    private List<String> photoUrls = new ArrayList<>();
    private String narration;
    private String audioPath;
    private int durationSeconds;
    private String langue;

    public VirtualTour() {
    }

    public VirtualTour(String lieu) {
        this.lieu = lieu;
    }

    // Getters et Setters
    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    public void addPhotoUrl(String url) {
        this.photoUrls.add(url);
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getLangue() {
        return langue;
    }

    public void setLangue(String langue) {
        this.langue = langue;
    }

    @Override
    public String toString() {
        return "VirtualTour{" +
                "lieu='" + lieu + '\'' +
                ", photos=" + photoUrls.size() +
                ", duration=" + durationSeconds + "s" +
                '}';
    }
}
