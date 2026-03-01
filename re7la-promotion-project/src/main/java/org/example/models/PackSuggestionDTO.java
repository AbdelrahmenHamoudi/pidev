package org.example.models;

/**
 * DTO for one AI-generated pack suggestion.
 * Read-only. Never persisted directly.
 *
 * City is extracted from offer names/lieu fields heuristically
 * (no dedicated city column exists in the schema).
 */
public class PackSuggestionDTO {

    private int        offer1Id;
    private int        offer2Id;
    private TargetType offer1Type;
    private TargetType offer2Type;
    private String     offer1Name;
    private String     offer2Name;
    private String     detectedCity;       // from offer title/lieu heuristic
    private int        frequency;          // co-reservation count
    private double     confidenceScore;    // 0..100
    private float      estimatedTotalPrice;
    private float      suggestedDiscount;  // percentage
    private String     suggestedName;
    private String     suggestedDescription;
    private String     aiReasoning;  // Claude's explanation for this suggestion

    // ── Getters ──────────────────────────────────────────────
    public int        getOffer1Id()             { return offer1Id; }
    public int        getOffer2Id()             { return offer2Id; }
    public TargetType getOffer1Type()           { return offer1Type; }
    public TargetType getOffer2Type()           { return offer2Type; }
    public String     getOffer1Name()           { return offer1Name; }
    public String     getOffer2Name()           { return offer2Name; }
    public String     getDetectedCity()         { return detectedCity; }
    public int        getFrequency()            { return frequency; }
    public double     getConfidenceScore()      { return confidenceScore; }
    public float      getEstimatedTotalPrice()  { return estimatedTotalPrice; }
    public float      getSuggestedDiscount()    { return suggestedDiscount; }
    public String     getSuggestedName()        { return suggestedName; }
    public String     getSuggestedDescription() { return suggestedDescription; }
    public String     getAiReasoning()          { return aiReasoning; }

    // ── Setters ──────────────────────────────────────────────
    public void setOffer1Id(int v)              { this.offer1Id = v; }
    public void setOffer2Id(int v)              { this.offer2Id = v; }
    public void setOffer1Type(TargetType v)     { this.offer1Type = v; }
    public void setOffer2Type(TargetType v)     { this.offer2Type = v; }
    public void setOffer1Name(String v)         { this.offer1Name = v; }
    public void setOffer2Name(String v)         { this.offer2Name = v; }
    public void setDetectedCity(String v)       { this.detectedCity = v; }
    public void setFrequency(int v)             { this.frequency = v; }
    public void setConfidenceScore(double v)    { this.confidenceScore = v; }
    public void setEstimatedTotalPrice(float v) { this.estimatedTotalPrice = v; }
    public void setSuggestedDiscount(float v)   { this.suggestedDiscount = v; }
    public void setSuggestedName(String v)      { this.suggestedName = v; }
    public void setSuggestedDescription(String v){ this.suggestedDescription = v; }
    public void setAiReasoning(String v)        { this.aiReasoning = v; }
}