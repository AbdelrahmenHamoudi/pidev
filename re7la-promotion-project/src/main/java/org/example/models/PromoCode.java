package org.example.models;

import java.sql.Timestamp;

/**
 * Représente un code promo lié à une promotion verrouillée.
 * Le code est généré par ZXing sous forme de QR Code.
 */
public class PromoCode {
    private int id;
    private int promotionId;
    private String code;        // ex: SUMMER2025-A3F9
    private String qrContent;   // JSON encodé dans le QR Code
    private boolean isUsed;
    private Integer usedBy;     // ID user
    private Timestamp usedAt;
    private Timestamp createdAt;

    public PromoCode() {}

    public PromoCode(int promotionId, String code, String qrContent) {
        this.promotionId = promotionId;
        this.code = code;
        this.qrContent = qrContent;
        this.isUsed = false;
    }

    // Getters / Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPromotionId() { return promotionId; }
    public void setPromotionId(int promotionId) { this.promotionId = promotionId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getQrContent() { return qrContent; }
    public void setQrContent(String qrContent) { this.qrContent = qrContent; }
    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }
    public Integer getUsedBy() { return usedBy; }
    public void setUsedBy(Integer usedBy) { this.usedBy = usedBy; }
    public Timestamp getUsedAt() { return usedAt; }
    public void setUsedAt(Timestamp usedAt) { this.usedAt = usedAt; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "PromoCode{id=" + id + ", code='" + code + "', promotionId=" + promotionId + ", isUsed=" + isUsed + '}';
    }
}