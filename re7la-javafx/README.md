# RE7LA - Module Activités JavaFX

## 📋 Description
Application desktop JavaFX pour la gestion des activités de voyage avec Back Office (administration) et Front Office (client).

## 🎨 Charte Graphique
- **Orange Principal**: #F39C12
- **Beige**: #FAFAFA
- **Jaune**: #F7DC6F
- **Bleu Foncé**: #2C3E50
- **Turquoise**: #1ABC9C

## 📁 Structure du Projet

```
re7la-javafx/
├── models/
│   ├── Activite.java          # Entité Activité
│   └── Planning.java           # Entité Planning
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

## 🚀 Fonctionnalités

### Back Office (Administration)
1. **Gestion des Activités**
   - ✅ CRUD complet (Créer, Lire, Modifier, Supprimer)
   - ✅ Recherche et filtres avancés (type, statut)
   - ✅ Statistiques en temps réel
   - ✅ Tableau avec actions rapides

2. **Gestion des Plannings**
   - ✅ Création de plannings pour chaque activité
   - ✅ Gestion des dates, horaires et places disponibles
   - ✅ Modification et suppression
   - ✅ Visualisation des statistiques

### Front Office (Client)
1. **Catalogue d'Activités**
   - ✅ Affichage en grille avec cards attractives
   - ✅ Filtres multiples (type, prix, lieu, disponibilité)
   - ✅ Recherche textuelle
   - ✅ Tri (prix, nom, popularité)

2. **Détails d'Activité**
   - ✅ Informations complètes
   - ✅ Liste des plannings disponibles
   - ✅ Système de réservation
   - ✅ Indicateurs de disponibilité

## 🔧 Installation et Configuration

### Prérequis
- Java JDK 11 ou supérieur
- JavaFX SDK 17 ou supérieur
- IDE (IntelliJ IDEA, Eclipse, NetBeans)

### Configuration dans votre projet

1. **Ajoutez JavaFX à votre projet**
   - Dans IntelliJ: File > Project Structure > Libraries > + > Java > Sélectionnez le dossier lib de JavaFX
   - Ou ajoutez dans pom.xml si vous utilisez Maven:

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.2</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>17.0.2</version>
</dependency>
```

2. **Copiez les fichiers dans votre projet**
   - Copiez `models/` dans `src/models/`
   - Copiez `controllers/` dans `src/controllers/`
   - Copiez `views/` dans `src/views/`
   - Copiez `resources/` dans `src/resources/`

3. **Configuration VM Options**
   Ajoutez ces options VM lors de l'exécution:
   ```
   --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```

## 💻 Utilisation

### Exemple de classe Main

Voir `MainApp.java` pour des exemples de lancement.

### Connexion à la Base de Données

Dans chaque contrôleur, remplacez les données de test par vos requêtes SQL:

```java
// TODO: Remplacer par votre service de base de données
// Exemple avec JDBC:
Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/re7la", "user", "password");
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM activitees");
ResultSet rs = stmt.executeQuery();

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
    activitesList.add(activite);
}
```

## 🎯 Points d'entrée

### Pour lancer le Back Office:
```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/ActiviteBackOffice.fxml"));
```

### Pour lancer le Front Office:
```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/ActiviteFrontOffice.fxml"));
```

## 🔄 Intégration avec votre Backend

### Service à créer
Créez un service pour gérer les opérations CRUD:

```java
public class ActiviteService {
    private Connection connection;
    
    public List<Activite> getAllActivites() { ... }
    public Activite getActiviteById(int id) { ... }
    public void addActivite(Activite activite) { ... }
    public void updateActivite(Activite activite) { ... }
    public void deleteActivite(int id) { ... }
}
```

Puis injectez ce service dans vos contrôleurs.

## 🎨 Personnalisation

### Modifier les couleurs
Éditez `resources/css/re7la-styles.css` et changez les valeurs hexadécimales.

### Ajouter des champs
1. Modifiez les classes entités (`Activite.java`, `Planning.java`)
2. Ajoutez les champs dans les formulaires FXML
3. Mettez à jour les contrôleurs correspondants

## 📝 Notes Importantes

- ⚠️ Les données actuelles sont des exemples. Remplacez-les par vos vraies requêtes SQL.
- 🔐 N'oubliez pas d'ajouter la validation des formulaires
- 📸 Pour les images, créez un dossier `resources/images/` et ajoutez vos images
- 🔗 Les relations entre Activité et Planning sont gérées par `id_activite`

## 🐛 Débogage

Si vous rencontrez des erreurs:
1. Vérifiez que JavaFX est correctement configuré
2. Vérifiez les chemins vers les fichiers FXML
3. Assurez-vous que les packages correspondent à votre structure
4. Vérifiez que le CSS est bien chargé

## 📞 Support

Pour toute question ou problème:
- Vérifiez que tous les imports sont corrects
- Assurez-vous que la structure des packages correspond
- Testez d'abord avec les données d'exemple avant d'intégrer la BD

## ✅ Checklist avant soumission

- [ ] Tous les fichiers sont copiés
- [ ] JavaFX est configuré
- [ ] Les packages correspondent à votre structure
- [ ] La connexion à la base de données est testée
- [ ] Les opérations CRUD fonctionnent
- [ ] L'interface est responsive
- [ ] Les styles sont appliqués correctement

Bon courage pour votre projet ! 🚀
