# 🚀 GUIDE DE DÉMARRAGE RAPIDE - RE7LA

## ⏱️ En 10 Minutes - Faire Fonctionner l'Application

### Étape 1: Copier les Fichiers (2 min)
```
Votre Projet/
└── src/
    ├── MainApp.java                    ← Copiez ce fichier
    ├── models/
    │   ├── Activite.java              ← Copiez
    │   └── Planning.java              ← Copiez
    ├── controllers/
    │   ├── backoffice/
    │   │   ├── ActiviteBackOfficeController.java
    │   │   └── PlanningBackOfficeController.java
    │   └── frontoffice/
    │       ├── ActiviteFrontOfficeController.java
    │       └── ActiviteDetailsController.java
    ├── views/
    │   ├── backoffice/
    │   │   ├── ActiviteBackOffice.fxml
    │   │   └── PlanningBackOffice.fxml
    │   └── frontoffice/
    │       ├── ActiviteFrontOffice.fxml
    │       └── ActiviteDetails.fxml
    └── resources/
        └── css/
            └── re7la-styles.css
```

### Étape 2: Configuration JavaFX (3 min)

#### Option A: IntelliJ IDEA
1. File → Project Structure → Libraries
2. Cliquez sur "+" → Java
3. Sélectionnez le dossier `lib` de votre JavaFX SDK
4. Cliquez OK

#### Option B: Eclipse
1. Right-click sur projet → Build Path → Configure Build Path
2. Libraries → Add External JARs
3. Sélectionnez tous les JAR dans `javafx-sdk/lib`

#### Option C: NetBeans
1. Right-click sur projet → Properties
2. Libraries → Add JAR/Folder
3. Ajoutez tous les JAR JavaFX

### Étape 3: VM Options (2 min)

Dans la configuration d'exécution, ajoutez:
```
--module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml
```

**Remplacez** `C:\path\to\javafx-sdk\lib` par le chemin réel vers votre JavaFX SDK.

### Étape 4: Tester l'Application (3 min)

1. **Ouvrez `MainApp.java`**
2. **Pour le Front Office** (recommandé pour tester):
   ```java
   launchFrontOffice(primaryStage); // ← Décommentez cette ligne
   ```
3. **Exécutez** MainApp.java
4. **Résultat attendu**: Une fenêtre avec le catalogue d'activités

---

## 🔧 INTÉGRATION AVEC VOTRE BASE DE DONNÉES

### Services à Créer

Créez un package `services/` avec ces classes:

#### ActiviteService.java
```java
package services;

import models.Activite;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteService {
    private Connection connection;
    
    public ActiviteService() {
        try {
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/re7la", 
                "root", 
                "password"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<Activite> getAllActivites() {
        List<Activite> activites = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM activitees");
            
            while (rs.next()) {
                Activite activite = new Activite(
                    rs.getInt("idActivite"),
                    rs.getString("nomA"),
                    rs.getString("descriptionA"),
                    rs.getString("lieu"),
                    rs.getFloat("prixParPersonne"),
                    rs.getInt("capaciteMax"),
                    rs.getString("type"),
                    rs.getString("statut"),
                    rs.getString("image")
                );
                activites.add(activite);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activites;
    }
    
    public void addActivite(Activite activite) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO activitees (nomA, descriptionA, lieu, prixParPersonne, capaciteMax, type, statut, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            );
            stmt.setString(1, activite.getNomA());
            stmt.setString(2, activite.getDescriptionA());
            stmt.setString(3, activite.getLieu());
            stmt.setFloat(4, activite.getPrixParPersonne());
            stmt.setInt(5, activite.getCapaciteMax());
            stmt.setString(6, activite.getType());
            stmt.setString(7, activite.getStatut());
            stmt.setString(8, activite.getImage());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateActivite(Activite activite) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE activitees SET nomA=?, descriptionA=?, lieu=?, prixParPersonne=?, " +
                "capaciteMax=?, type=?, statut=?, image=? WHERE idActivite=?"
            );
            stmt.setString(1, activite.getNomA());
            stmt.setString(2, activite.getDescriptionA());
            stmt.setString(3, activite.getLieu());
            stmt.setFloat(4, activite.getPrixParPersonne());
            stmt.setInt(5, activite.getCapaciteMax());
            stmt.setString(6, activite.getType());
            stmt.setString(7, activite.getStatut());
            stmt.setString(8, activite.getImage());
            stmt.setInt(9, activite.getIdActivite());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void deleteActivite(int id) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM activitees WHERE idActivite=?"
            );
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

#### PlanningService.java
```java
package services;

import models.Planning;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PlanningService {
    private Connection connection;
    
    public PlanningService() {
        try {
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/re7la", 
                "root", 
                "password"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<Planning> getPlanningsByActivite(int idActivite) {
        List<Planning> plannings = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM planing WHERE id_activite=?"
            );
            stmt.setInt(1, idActivite);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Planning planning = new Planning(
                    rs.getInt("idPlanning"),
                    rs.getInt("id_activite"),
                    rs.getDate("datePlanning").toLocalDate(),
                    rs.getTime("heureDebut").toLocalTime(),
                    rs.getTime("heureFin").toLocalTime(),
                    rs.getString("etat"),
                    rs.getInt("nbPlacesRestantes")
                );
                plannings.add(planning);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plannings;
    }
    
    public void addPlanning(Planning planning) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO planing (id_activite, datePlanning, heureDebut, heureFin, etat, nbPlacesRestantes) " +
                "VALUES (?, ?, ?, ?, ?, ?)"
            );
            stmt.setInt(1, planning.getIdActivite());
            stmt.setDate(2, Date.valueOf(planning.getDatePlanning()));
            stmt.setTime(3, Time.valueOf(planning.getHeureDebut()));
            stmt.setTime(4, Time.valueOf(planning.getHeureFin()));
            stmt.setString(5, planning.getEtat());
            stmt.setInt(6, planning.getNbPlacesRestantes());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### Utiliser les Services dans les Contrôleurs

**Dans ActiviteBackOfficeController.java**, remplacez:
```java
// Au lieu de:
private void loadActivitesData() {
    activitesList.add(new Activite(...)); // Données test
}

// Utilisez:
private ActiviteService activiteService = new ActiviteService();

private void loadActivitesData() {
    activitesList.clear();
    activitesList.addAll(activiteService.getAllActivites());
    filteredList.addAll(activitesList);
}
```

---

## 🎯 CHECKLIST DE VÉRIFICATION

Avant de tester:
- [ ] JavaFX SDK téléchargé et configuré
- [ ] Tous les fichiers copiés dans le bon package
- [ ] VM Options configurées
- [ ] MySQL Connector ajouté au projet (si vous utilisez MySQL)
- [ ] Base de données créée avec les bonnes tables

---

## 🐛 PROBLÈMES COURANTS

### Erreur: "Error: JavaFX runtime components are missing"
**Solution**: Ajoutez les VM Options (voir Étape 3)

### Erreur: "Location is required"
**Solution**: Vérifiez le chemin dans FXMLLoader, doit être `/views/...` pas `views/...`

### Erreur: NullPointerException sur @FXML
**Solution**: Vérifiez que fx:id dans FXML correspond au nom de variable dans le contrôleur

### Interface ne s'affiche pas correctement
**Solution**: Vérifiez que `stylesheets="@../../resources/css/re7la-styles.css"` est correct

### Connexion BD échoue
**Solution**: Vérifiez:
1. MySQL est démarré
2. Nom de la base de données est correct
3. Username et password sont corrects
4. MySQL Connector est dans le classpath

---

## 📞 AIDE RAPIDE

**Pour afficher le Back Office activités:**
```java
launchBackOffice(primaryStage);
```

**Pour afficher le Front Office catalogue:**
```java
launchFrontOffice(primaryStage);
```

**Pour afficher la gestion des plannings:**
```java
launchPlanningBackOffice(primaryStage);
```

---

## ⚡ TIPS

1. **Commencez par le Front Office** - plus visuel, plus facile à tester
2. **Testez avec les données d'exemple** avant d'intégrer la BD
3. **Une interface à la fois** - ne lancez pas tout en même temps
4. **Utilisez les statistiques** pour vérifier que les données se chargent

Bonne chance! 🚀
