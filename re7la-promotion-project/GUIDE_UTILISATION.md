# 📘 GUIDE D'UTILISATION COMPLET - RE7LA PROMOTIONS

## 📑 Table des Matières
1. [Vue d'ensemble](#vue-densemble)
2. [Installation](#installation)
3. [Utilisation BackOffice](#utilisation-backoffice)
4. [Utilisation FrontOffice](#utilisation-frontoffice)
5. [Migration vers Base de Données](#migration-vers-base-de-données)
6. [Personnalisation](#personnalisation)
7. [Dépannage](#dépannage)

---

## 1. Vue d'ensemble

### 🎯 Objectif du Projet
Gérer les promotions et packs promotionnels pour l'application RE7LA (Voyage & Loisirs).

### 🔑 Concepts Clés

#### **Promotion Individuelle**
- S'applique sur UN seul type d'offre
- Types: Hébergement, Activité, ou Transport
- Exemple: "-20% sur tous les hébergements à Sousse"

#### **Pack Promotion**
- Combine 2 ou 3 types d'offres
- Exemple: "Hébergement + Activité à -150 TND"
- Exemple: "Hébergement + Activité + Transport à -25%"

#### **Types de Réductions**
1. **Pourcentage** : Réduction en % (ex: 20%)
2. **Fixe** : Réduction en TND (ex: 150 TND)
3. **Au moins un** des deux doit être renseigné

---

## 2. Installation

### Prérequis
```
✅ Java JDK 17 ou supérieur
✅ Maven 3.8+
✅ IDE (IntelliJ IDEA recommandé)
✅ JavaFX 21+
```

### Étapes d'Installation

#### Étape 1: Importer le Projet
```bash
# Si vous utilisez Git
git clone <url-du-repo>
cd re7la-promotion-project

# Ou simplement ouvrir le dossier dans votre IDE
```

#### Étape 2: Configuration Maven
```bash
# Télécharger les dépendances
mvn clean install
```

#### Étape 3: Configuration JavaFX (IntelliJ IDEA)

1. **File → Project Structure → Libraries**
2. **Add → Java → Sélectionnez javafx-sdk/lib**
3. **Run → Edit Configurations**
4. **Add VM Options:**
```
--module-path "chemin/vers/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml
```

#### Étape 4: Ajouter le Logo
- Placez votre fichier `logo.png` dans:
  ```
  src/main/resources/images/logo.png
  ```

---

## 3. Utilisation BackOffice

### Lancement
```bash
# Avec Maven
mvn javafx:run

# Ou exécuter directement
MainApp.java
```

### Interface Admin

#### 📊 Dashboard
- **Total Promotions** : Nombre total de promotions
- **Packs Promo** : Nombre de packs combinés
- **Actives** : Promotions en cours de validité

#### ➕ Créer une Promotion

**Champs Obligatoires:**
- Nom de la promotion
- Description
- Date de début
- Date de fin
- **Au moins une** réduction (% OU fixe)

**Workflow:**
```
1. Remplir le formulaire
2. Cocher "Pack" si combinaison d'offres
3. Cliquer "💾 Enregistrer"
4. (Optionnel) Ajouter des offres:
   - Choisir le type (Hébergement/Activité/Transport)
   - Sélectionner une offre dans la liste
   - Cliquer "➕ Ajouter cette offre"
```

#### ✏️ Modifier une Promotion

```
1. Cliquer sur une ligne dans le tableau
2. Les champs se remplissent automatiquement
3. Modifier les valeurs
4. Cliquer "💾 Modifier la promotion"
```

#### 🗑️ Supprimer une Promotion

```
1. Sélectionner une promotion dans le tableau
2. Cliquer "🗑️ Supprimer"
3. Confirmer l'action
```

### 🔍 Fonctionnalités Avancées

#### Recherche
- Utilisez la barre de recherche en haut
- Recherche par nom ou description

#### Filtrage
- Tableau triable par colonne
- Click sur l'en-tête de colonne pour trier

---

## 4. Utilisation FrontOffice

### Lancement
```bash
# Exécuter
MainAppFrontOffice.java
```

### Interface Utilisateur

#### 🔎 Filtres Disponibles

**Type de Promotion:**
- ☑️ Packs combinés
- ☑️ Promotions individuelles

**Disponibilité:**
- ☑️ Uniquement actives (en cours de validité)

**Tri:**
- Plus récentes
- Réduction croissante
- Réduction décroissante

#### 📋 Cartes de Promotions

**Informations Affichées:**
- 🏷️ Nom et description
- 💰 Réduction (% ou TND)
- 📅 Dates de validité
- 📦 Type (Pack ou Individuelle)
- 🎯 Composants (pour les packs)

#### 🎫 Réserver une Promotion

```
1. Trouver la promotion souhaitée
2. Cliquer "Réserver cette promotion ➜"
3. (À implémenter: processus de réservation)
```

---

## 5. Migration vers Base de Données

### Pourquoi Migrer?
- ✅ Persistance des données (actuellement en mémoire)
- ✅ Performance sur grandes quantités
- ✅ Partage entre plusieurs utilisateurs
- ✅ Sécurité et sauvegarde

### Étapes de Migration

#### Étape 1: Créer la Base de Données
```bash
# Connectez-vous à MySQL
mysql -u root -p

# Exécutez le script
source database.sql
```

#### Étape 2: Configurer la Connexion
Éditez `utils/DatabaseConnection.java`:
```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/re7la_db";
private static final String DB_USER = "votre_user";
private static final String DB_PASSWORD = "votre_password";
```

#### Étape 3: Tester la Connexion
```bash
# Exécutez
java org.example.utils.DatabaseConnection
```

#### Étape 4: Remplacer les Services
```
1. Renommez PromotionService.java → PromotionServiceOld.java
2. Renommez PromotionServiceWithDB.java → PromotionService.java
3. Faites de même pour PromotionTargetService
4. Relancez l'application
```

---

## 6. Personnalisation

### 🎨 Changer les Couleurs

Éditez `re7la-styles.css`:
```css
/* Couleur principale (Orange) */
#F39C12 → Votre couleur

/* Couleur secondaire (Turquoise) */
#1ABC9C → Votre couleur

/* Couleur de fond */
#eef2f6 → Votre couleur
```

### 🖼️ Changer le Logo

Remplacez simplement:
```
src/main/resources/images/logo.png
```
Recommandé: 150x150px, format PNG avec fond transparent

### ➕ Ajouter des Champs

**Dans l'entité (`models/Promotion.java`):**
```java
private String nouveauChamp;

public String getNouveauChamp() { return nouveauChamp; }
public void setNouveauChamp(String nouveauChamp) { this.nouveauChamp = nouveauChamp; }
```

**Dans le FXML (`views/backoffice/PromotionBackOffice.fxml`):**
```xml
<VBox spacing="6.0">
    <Label styleClass="form-label" text="Nouveau Champ" />
    <TextField fx:id="txtNouveauChamp" styleClass="text-field" />
</VBox>
```

**Dans le Controller:**
```java
@FXML private TextField txtNouveauChamp;

// Dans addOrUpdate()
promotion.setNouveauChamp(txtNouveauChamp.getText());

// Dans fillFormWithPromotion()
txtNouveauChamp.setText(promo.getNouveauChamp());
```

---

## 7. Dépannage

### ❌ Erreur: "Module javafx.controls not found"

**Solution:**
```
1. Vérifiez l'installation de JavaFX
2. Ajoutez les VM options:
   --module-path "chemin/javafx-sdk/lib" 
   --add-modules javafx.controls,javafx.fxml
```

### ❌ CSS ne se charge pas

**Solution:**
```
1. Vérifiez le chemin dans FXML: @../../css/re7la-styles.css
2. Vérifiez que le fichier existe dans src/main/resources/css/
3. Rebuild le projet: mvn clean install
```

### ❌ Logo ne s'affiche pas

**Solution:**
```
1. Vérifiez que logo.png existe dans src/main/resources/images/
2. Vérifiez le chemin dans FXML: @../../images/logo.png
3. Le fichier doit être PNG (pas JPG)
```

### ❌ Erreur de connexion MySQL

**Solution:**
```
1. Vérifiez que MySQL est démarré
2. Vérifiez URL, USER, PASSWORD dans DatabaseConnection.java
3. Vérifiez que la base 're7la_db' existe
4. Testez: java org.example.utils.DatabaseConnection
```

### ❌ TableView ne se remplit pas

**Solution:**
```
1. Vérifiez les PropertyValueFactory dans setupPromotionTable()
2. Les noms doivent correspondre aux getters (ex: "name" → getName())
3. Ajoutez des System.out.println() pour déboguer
```

---

## 📞 Support

Pour toute question ou problème:
1. Vérifiez ce guide
2. Consultez le README.md
3. Examinez les commentaires dans le code
4. Testez avec les données d'exemple fournies

---

**Bon développement ! 🚀**
