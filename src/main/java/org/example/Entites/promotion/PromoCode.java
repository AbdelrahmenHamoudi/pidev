package org.example.Entites.promotion;

import org.example.Entites.user.User;
import java.sql.Timestamp;

public class PromoCode {
    private int id;
    private int promotionId;
    private String code;
    private String qrContent;
    private boolean isUsed;
    private User usedBy;        // ✅ Changé de int à User
    private Timestamp usedAt;
    private Timestamp createdAt;

    public PromoCode() {}

    public PromoCode(int promotionId, String code, String qrContent) {
        this.promotionId = promotionId;
        this.code = code;
        this.qrContent = qrContent;
        this.isUsed = false;
    }

    // Getters et Setters
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

    public User getUsedBy() { return usedBy; }        // ✅ Getter User
    public void setUsedBy(User usedBy) { this.usedBy = usedBy; }  // ✅ Setter User

    public Timestamp getUsedAt() { return usedAt; }
    public void setUsedAt(Timestamp usedAt) { this.usedAt = usedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}