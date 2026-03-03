package org.example.Entites.promotion;

import java.util.Objects;

public class PromotionTarget {

    private int id;
    private int promotionId;
    private TargetType targetType;
    private int targetId;               // id_hebergement, id_activité, id_transport...

    public PromotionTarget() {
    }

    public PromotionTarget(int promotionId, TargetType targetType, int targetId) {
        this.promotionId = promotionId;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public PromotionTarget(int id, int promotionId, TargetType targetType, int targetId) {
        this.id = id;
        this.promotionId = promotionId;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPromotionId() { return promotionId; }
    public void setPromotionId(int promotionId) { this.promotionId = promotionId; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }

    @Override
    public String toString() {
        return "PromotionTarget{" +
                "id=" + id +
                ", promotionId=" + promotionId +
                ", targetType=" + targetType +
                ", targetId=" + targetId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromotionTarget that = (PromotionTarget) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
