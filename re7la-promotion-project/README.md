# RE7LA - Système de Gestion des Promotions 🎁

Application JavaFX pour la gestion des promotions et packs promotionnels dans le cadre du projet RE7LA (Voyage & Loisirs).

## 📋 Description

Cette application permet de :
- **BackOffice (Admin)** : Créer, modifier, supprimer des promotions individuelles ou des packs combinés
- **FrontOffice (User)** : Consulter et réserver les promotions disponibles

### Types de Promotions
- **Promotions Individuelles** : Réduction sur hébergement, activité ou transport
- **Packs Promotionnels** : Combinaison de 2 ou 3 offres (ex: hébergement + transport)

## 🏗️ Structure du Projet

```
re7la-promotion-project/
├── src/main/java/org/example/
│   ├── models/                          # Entités
│   │   ├── Promotion.java
│   │   ├── PromotionTarget.java
│   │   ├── TargetType.java (enum)
│   │   ├── Hebergement.java
│   │   ├── Activite.java
│   │   └── Transport.java
│   ├── services/                        # Logique métier
│   │   ├── PromotionService.java
│   │   ├── PromotionTargetService.java
│   │   └── OffresStaticData.java
│   ├── controllers/
│   │   ├── backoffice/
│   │   │   └── PromotionBackOfficeController.java
│   │   └── frontoffice/
│   │       └── PromotionFrontOfficeController.java
│   ├── MainApp.java                     # Lancement BackOffice
│   └── MainAppFrontOffice.java          # Lancement FrontOffice
├── src/main/resources/
│   ├── views/
│   │   ├── backoffice/
│   │   │   └── PromotionBackOffice.fxml
│   │   └── frontoffice/
│   │       └── PromotionFrontOffice.fxml
│   ├── css/
│   │   └── re7la-styles.css
│   └── images/
│       └── logo.png
└── pom.xml
```

## 🚀 Installation et Lancement

### Prérequis
- Java 17 ou supérieur
- Maven 3.8+
- JavaFX 21+

### Étapes

1. **Importer le projet dans votre IDE** (IntelliJ IDEA, Eclipse, VSCode)

2. **Ajouter votre logo** :
   - Placez votre logo `logo.png` dans `src/main/resources/images/`

3. **Compiler le projet** :
   ```bash
   mvn clean install
   ```

4. **Lancer le BackOffice** (Admin) :
   ```bash
   mvn javafx:run
   ```
   Ou exécutez directement la classe `MainApp.java`

5. **Lancer le FrontOffice** (User) :
   - Modifiez temporairement le `pom.xml` : `<mainClass>org.example.MainAppFrontOffice</mainClass>`
   - Ou exécutez directement la classe `MainAppFrontOffice.java`

## 📊 Données Statiques

Pour le moment, l'application utilise des **données statiques** (ArrayList) pour :
- Promotions (3 exemples pré-chargés)
- Hébergements (5 exemples)
- Activités (5 exemples)
- Transports (5 exemples)

Ces données sont initialisées dans les services respectifs.

## 🎨 Design

Le projet utilise la charte graphique RE7LA :
- **Orange vif** (`#F39C12`) : Boutons, accents
- **Jaune pastel** (`#F7DC6F`) : Surbrillances
- **Bleu nuit** (`#2C3E50`) : Texte, fond sidebar
- **Turquoise** (`#1ABC9C`) : Succès, validation

## 🔄 Migration vers Base de Données

Pour connecter à une vraie base de données MySQL :

1. **Créer les tables** :
```sql
CREATE TABLE promotion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    discount_percentage FLOAT,
    discount_fixed FLOAT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_pack BOOLEAN DEFAULT FALSE
);

CREATE TABLE promotion_target (
    id INT PRIMARY KEY AUTO_INCREMENT,
    promotion_id INT NOT NULL,
    target_type ENUM('HEBERGEMENT', 'ACTIVITE', 'TRANSPORT'),
    target_id INT NOT NULL,
    FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE
);
```

2. **Créer une classe `DatabaseConnection`** :
```java
public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/re7la_db";
    private static final String USER = "root";
    private static final String PASSWORD = "votre_mot_de_passe";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

3. **Modifier les Services** : Remplacer les ArrayList par des requêtes SQL

## 🎯 Fonctionnalités

### BackOffice (Admin)
- ✅ Créer une promotion (individuelle ou pack)
- ✅ Modifier une promotion existante
- ✅ Supprimer une promotion
- ✅ Sélectionner des offres (hébergement/activité/transport) à associer
- ✅ Visualiser toutes les promotions dans un tableau
- ✅ Filtrage et recherche

### FrontOffice (User)
- ✅ Consulter toutes les promotions disponibles
- ✅ Filtrer par type (pack/individuel)
- ✅ Filtrer par disponibilité (actives uniquement)
- ✅ Rechercher une promotion
- ✅ Trier les résultats
- ✅ Visualiser les détails d'une promotion
- 🔄 Réserver une promotion (à implémenter)

## 📝 Notes Importantes

- Les champs `discountPercentage` et `discountFixed` sont optionnels mais **au moins un des deux** doit être renseigné
- Un pack doit avoir au moins 2 offres associées
- Les dates de début et fin sont obligatoires
- Le logo doit être au format PNG (recommandé : 150x150px)

## 🐛 Résolution de Problèmes

### Erreur "Module not found"
- Vérifiez que JavaFX est bien configuré dans votre IDE
- Ajoutez les VM options : `--module-path "chemin/vers/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml`

### CSS ne se charge pas
- Vérifiez le chemin dans le FXML : `@../../css/re7la-styles.css`
- Assurez-vous que le fichier CSS est bien dans `src/main/resources/css/`

### Logo ne s'affiche pas
- Placez `logo.png` dans `src/main/resources/images/`
- Vérifiez le chemin : `@../../images/logo.png`

## 👨‍💻 Auteur

Projet développé dans le cadre de RE7LA - Application de Voyage et Loisirs

## 📄 Licence

Projet académique - Tous droits réservés

---

**Bon courage pour votre validation ! 🚀**
