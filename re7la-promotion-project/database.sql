-- ============================================
-- RE7LA - Script de Création de Base de Données
-- Module: Promotions
-- ============================================

-- Création de la base de données
CREATE DATABASE IF NOT EXISTS re7la_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE re7la_db;

-- ============================================
-- Table: promotion
-- Description: Stocke les promotions et packs
-- ============================================
CREATE TABLE IF NOT EXISTS promotion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT 'Nom de la promotion',
    description TEXT COMMENT 'Description détaillée',
    discount_percentage FLOAT NULL COMMENT 'Réduction en pourcentage (ex: 20 pour 20%)',
    discount_fixed FLOAT NULL COMMENT 'Réduction fixe en TND',
    start_date DATE NOT NULL COMMENT 'Date de début de validité',
    end_date DATE NOT NULL COMMENT 'Date de fin de validité',
    is_pack BOOLEAN DEFAULT FALSE COMMENT 'True si pack combiné, False si promo individuelle',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_discount CHECK (
        (discount_percentage IS NOT NULL AND discount_percentage > 0) OR 
        (discount_fixed IS NOT NULL AND discount_fixed > 0)
    ),
    CONSTRAINT chk_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB COMMENT='Table des promotions';

-- ============================================
-- Table: promotion_target
-- Description: Lie les promotions aux offres (hébergement, activité, transport)
-- ============================================
CREATE TABLE IF NOT EXISTS promotion_target (
    id INT PRIMARY KEY AUTO_INCREMENT,
    promotion_id INT NOT NULL COMMENT 'ID de la promotion',
    target_type ENUM('HEBERGEMENT', 'ACTIVITE', 'TRANSPORT') NOT NULL COMMENT 'Type de cible',
    target_id INT NOT NULL COMMENT 'ID de l offre ciblée',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE,
    
    INDEX idx_promotion (promotion_id),
    INDEX idx_target_type (target_type),
    INDEX idx_target_id (target_id)
) ENGINE=InnoDB COMMENT='Table de liaison promotion-offres';

-- ============================================
-- Insertion de Données de Test
-- ============================================

-- Promotions individuelles
INSERT INTO promotion (name, description, discount_percentage, discount_fixed, start_date, end_date, is_pack) VALUES
('Été 2025', 'Réduction spéciale pour profiter de l été tunisien', 20.0, NULL, '2025-06-01', '2025-08-31', FALSE),
('Black Friday', 'Méga réduction pour le Black Friday', 30.0, NULL, '2025-11-20', '2025-11-27', FALSE),
('Hiver Doux', 'Offre spéciale hiver', NULL, 100.0, '2025-12-01', '2026-02-28', FALSE);

-- Pack promotionnel
INSERT INTO promotion (name, description, discount_percentage, discount_fixed, start_date, end_date, is_pack) VALUES
('Pack Aventure', 'Hébergement + Activité à prix réduit', NULL, 150.0, '2025-05-01', '2025-09-30', TRUE),
('Pack Complet', 'Hébergement + Activité + Transport tout inclus', 25.0, NULL, '2025-07-01', '2025-08-31', TRUE);

-- Targets pour "Été 2025" (promo individuelle sur hébergements)
INSERT INTO promotion_target (promotion_id, target_type, target_id) VALUES
(1, 'HEBERGEMENT', 1),
(1, 'HEBERGEMENT', 2),
(1, 'HEBERGEMENT', 3);

-- Targets pour "Pack Aventure" (hébergement + activité)
INSERT INTO promotion_target (promotion_id, target_type, target_id) VALUES
(4, 'HEBERGEMENT', 1),
(4, 'ACTIVITE', 2);

-- Targets pour "Pack Complet" (hébergement + activité + transport)
INSERT INTO promotion_target (promotion_id, target_type, target_id) VALUES
(5, 'HEBERGEMENT', 3),
(5, 'ACTIVITE', 1),
(5, 'TRANSPORT', 1);

-- ============================================
-- Vues Utiles
-- ============================================

-- Vue: Promotions actives
CREATE OR REPLACE VIEW v_promotions_actives AS
SELECT 
    p.*,
    CASE 
        WHEN CURDATE() BETWEEN p.start_date AND p.end_date THEN 'Active'
        WHEN CURDATE() < p.start_date THEN 'À venir'
        ELSE 'Expirée'
    END AS statut
FROM promotion p;

-- Vue: Détails des packs avec leurs composants
CREATE OR REPLACE VIEW v_packs_details AS
SELECT 
    p.id AS promotion_id,
    p.name AS promotion_name,
    p.description,
    pt.target_type,
    pt.target_id,
    COUNT(pt.id) OVER (PARTITION BY p.id) AS nb_composants
FROM promotion p
INNER JOIN promotion_target pt ON p.id = pt.promotion_id
WHERE p.is_pack = TRUE;

-- ============================================
-- Requêtes Utiles
-- ============================================

-- Lister toutes les promotions actives
-- SELECT * FROM v_promotions_actives WHERE statut = 'Active';

-- Trouver tous les packs
-- SELECT * FROM promotion WHERE is_pack = TRUE;

-- Compter les promotions par type
-- SELECT 
--     CASE WHEN is_pack THEN 'Pack' ELSE 'Individuelle' END AS type,
--     COUNT(*) as nombre
-- FROM promotion
-- GROUP BY is_pack;

-- Promotions avec le plus de réduction
-- SELECT 
--     name,
--     COALESCE(discount_percentage, 0) as reduction_pourcent,
--     COALESCE(discount_fixed, 0) as reduction_fixe
-- FROM promotion
-- ORDER BY GREATEST(COALESCE(discount_percentage, 0), COALESCE(discount_fixed, 0)) DESC;

-- ============================================
-- Procédures Stockées (Optionnel)
-- ============================================

DELIMITER //

CREATE PROCEDURE sp_get_promotion_with_targets(IN promo_id INT)
BEGIN
    SELECT 
        p.*,
        pt.target_type,
        pt.target_id
    FROM promotion p
    LEFT JOIN promotion_target pt ON p.id = pt.promotion_id
    WHERE p.id = promo_id;
END //

CREATE PROCEDURE sp_delete_expired_promotions()
BEGIN
    DELETE FROM promotion 
    WHERE end_date < CURDATE() 
    AND DATEDIFF(CURDATE(), end_date) > 90; -- Supprimer après 90 jours d'expiration
END //

DELIMITER ;

-- ============================================
-- Index pour Performance
-- ============================================

CREATE INDEX idx_promotion_dates ON promotion(start_date, end_date);
CREATE INDEX idx_promotion_pack ON promotion(is_pack);
CREATE INDEX idx_promotion_name ON promotion(name);

-- ============================================
-- Fin du Script
-- ============================================

SELECT 'Base de données RE7LA créée avec succès!' AS message;
