package org.example.models;

/**
 * ✅ FIXED: TRANSPORT → VOITURE to match the real table name in re7la_3a9.
 *
 * DB table is called `voiture`, not `transport`.
 * The promotion_target SQL migration script updates the ENUM accordingly.
 */
public enum TargetType {
    HEBERGEMENT,
    ACTIVITE,
    VOITURE
}