package org.example.Services.activite;

import org.example.Entites.activite.VirtualTour;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 🎥 Service de génération de visites virtuelles - VERSION AMÉLIORÉE
 *
 * Améliorations :
 *  1. PHOTOS  → Wikimedia Commons en priorité (images libres, très précises)
 *               + Unsplash/Pexels en fallback
 *  2. NARRATION → Prompt enrichi avec contexte culturel + modèle llama-3.3-70b
 */
public class VirtualTourServiceImpl {

    private String unsplashApiKey;
    private String groqApiKey;
    private static final String PEXELS_API_KEY = "563492ad6f91700001000001c4d4b6e35f774f2b9d68f16dd1e33e02";

    // ── Contexte culturel par lieu ──────────────────────────────────────────────
    // Enrichit le prompt pour que l'AI génère une narration précise et vivante
    private static final Map<String, String> LIEU_CONTEXT = new HashMap<>();
    static {
        LIEU_CONTEXT.put("sidi bou said",
                "Village emblématique au sommet d'une falaise surplombant le golfe de Tunis. " +
                        "Connu pour ses maisons aux façades blanche immaculée et portes bleues cobalt, " +
                        "ses ruelles pavées, la vue panoramique depuis le café des Délices, " +
                        "l'influence andalouse dans l'architecture, les bougainvilliers roses, " +
                        "le palais Baron d'Erlanger, et l'ambiance bohème qui a inspiré peintres et écrivains " +
                        "dont Paul Klee et Simone de Beauvoir.");

        LIEU_CONTEXT.put("carthage",
                "Ancienne métropole phénicienne fondée en 814 av. J.-C., rivale de Rome. " +
                        "Site classé UNESCO comprenant les thermes d'Antonin (les plus grands d'Afrique), " +
                        "le tophet (sanctuaire phénicien), le port circulaire militaire, " +
                        "l'amphithéâtre, les villas des Bords de Mer, et le musée national de Carthage. " +
                        "Détruite par Rome en 146 av. J.-C. puis reconstruite en cité romaine prospère.");

        LIEU_CONTEXT.put("djerba",
                "Île magique du sud tunisien surnommée 'l'île du bonheur'. " +
                        "Connue pour la synagogue de la Ghriba (pèlerinage annuel), " +
                        "le village de potiers de Guellala, Houmt Souk et ses fondouks, " +
                        "les plages de Sidi Mahrez, l'huile d'olive locale, " +
                        "la communauté juive millénaire, et l'architecture berbère blanche.");

        LIEU_CONTEXT.put("médina de tunis",
                "Médina classée UNESCO, l'une des plus belles du monde arabe. " +
                        "Fondée au VIIe siècle, elle abrite la Grande Mosquée Zitouna (737 ap. J.-C.), " +
                        "des souks spécialisés (chéchias, parfums, cuivre), " +
                        "des palais beys, des médersas, et plus de 700 monuments historiques. " +
                        "Labyrinthe de ruelles où artisans et marchands perpétuent des traditions millénaires.");

        LIEU_CONTEXT.put("douz",
                "Porte du Sahara tunisien, surnommée 'la reine du désert'. " +
                        "Point de départ des excursions vers les grandes dunes de l'erg oriental, " +
                        "marché hebdomadaire berbère authentique, Festival International du Sahara, " +
                        "oasis de 400 000 palmiers-dattiers, et traditions nomades touaregs préservées.");

        LIEU_CONTEXT.put("kairouan",
                "Quatrième ville sainte de l'Islam. Fondée en 670 par Oqba Ibn Nafi. " +
                        "La Grande Mosquée de Kairouan (une des plus anciennes au monde), " +
                        "la Mosquée des Trois Portes, les Bassins des Aghlabides, " +
                        "le mausolée de Sidi Sahbi, les célèbres makhroudh (pâtisseries au dattes), " +
                        "et les tapis berbères aux motifs géométriques uniques.");

        LIEU_CONTEXT.put("hammamet",
                "Station balnéaire emblématique du nord-est tunisien, perle de la Méditerranée. " +
                        "Connue pour ses plages de sable fin, la médina fortifiée du XVe siècle, " +
                        "le théâtre antique de Pupput, les jardins de la Villa Sébastien, " +
                        "la citadelle byzantine et le souk animé. " +
                        "Destination de villégiature prisée depuis les années 60.");

        LIEU_CONTEXT.put("sousse",
                "Troisième ville de Tunisie, surnommée 'la Perle du Sahel'. " +
                        "Médina classée UNESCO avec sa Grande Mosquée (851 ap. J.-C.), " +
                        "la ribat forteresse du VIIIe siècle, le musée archéologique " +
                        "abritant la mosaïque de Méduse, le port de pêche animé et " +
                        "le quartier Boujaffar face à la mer.");

        LIEU_CONTEXT.put("monastir",
                "Cité des Aghlabides sur la côte du Sahel. " +
                        "Le ribat de Monastir (IXe siècle) est l'un des plus beaux de Tunisie, " +
                        "décor de nombreux films dont 'La Vie de Brian'. " +
                        "Mausolée Bourguiba, médina, marina moderne, " +
                        "et plages de sable blanc face aux îles Kuriat.");

        LIEU_CONTEXT.put("el jem",
                "Amphithéâtre romain d'El Jem, classé UNESCO, troisième plus grand " +
                        "amphithéâtre du monde romain (capacité 35 000 spectateurs). " +
                        "Construit au IIIe siècle sous Gordien Ier. " +
                        "Festival international de musique symphonique chaque été, " +
                        "musée archéologique avec mosaïques exceptionnelles.");

        LIEU_CONTEXT.put("tozeur",
                "Porte du désert et capitale du Djérid. " +
                        "Architecture en briques de pisé aux motifs géométriques uniques, " +
                        "palmeraie de 400 000 palmiers, oasis de Chebika et Tamerza, " +
                        "chotts salés aux reflets roses au coucher du soleil. " +
                        "Décors naturels utilisés pour Star Wars (Tatooine), " +
                        "dattes Deglet Nour réputées mondialement.");

        LIEU_CONTEXT.put("tabarka",
                "Ville côtière du nord-ouest tunisien, entre mer et forêts de pins. " +
                        "Fort génois du XVIe siècle sur un îlot rocheux, " +
                        "récifs coralliens exceptionnels pour la plongée, " +
                        "festival international de jazz en été, artisanat du corail rouge, " +
                        "parc national d'Ichkeul (UNESCO) tout proche.");

        LIEU_CONTEXT.put("bulla regia",
                "Site archéologique romain unique au monde. " +
                        "Ses habitants avaient creusé des villas souterraines pour fuir la chaleur, " +
                        "conservant des mosaïques polychromes d'une qualité exceptionnelle. " +
                        "Théâtre, thermes, capitole et forum encore bien préservés. " +
                        "Période de prospérité sous les Antonins (IIe siècle ap. J.-C.).");

        // Fallback générique Tunisie
        LIEU_CONTEXT.put("default",
                "Site touristique de Tunisie, pays au carrefour des civilisations " +
                        "berbère, phénicienne, romaine, arabo-islamique et ottomane. " +
                        "Patrimoine UNESCO, gastronomie méditerranéenne, hospitalité légendaire.");
    }

    public VirtualTourServiceImpl() {
        loadApiKeys();
    }

    private void loadApiKeys() {
        try {
            Properties props = new Properties();
            InputStream input = getClass().getClassLoader()
                    .getResourceAsStream("api-keys.properties");
            if (input != null) {
                props.load(input);
                unsplashApiKey = props.getProperty("unsplash.api.key");
                groqApiKey = props.getProperty("groq.api.key");
                System.out.println("✅ Clés API chargées");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement clés : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ═══════════════════════════════════════════════════════════════════════════

    public VirtualTour generateTour(String lieu, String langue) {
        VirtualTour tour = new VirtualTour(lieu);
        tour.setLangue(langue);

        System.out.println("🎬 === GÉNÉRATION VISITE VIRTUELLE ===");
        System.out.println("📍 Lieu : " + lieu + " | 🗣️  Langue : " + langue);

        try {
            // 1. Photos
            System.out.println("\n📸 === RÉCUPÉRATION PHOTOS ===");
            List<String> photos = fetchDiversePhotos(lieu);
            tour.setPhotoUrls(photos);
            System.out.println("✅ " + photos.size() + " photos récupérées");
            photos.forEach(u -> System.out.println("  → " + u.substring(0, Math.min(80, u.length()))));

            // 2. Narration
            System.out.println("\n🤖 === GÉNÉRATION NARRATION ===");
            String narration = generateNarrationWithGroq(lieu, langue);
            tour.setNarration(narration);
            System.out.println("✅ Narration (" + narration.length() + " car.) :");
            System.out.println("   " + narration.substring(0, Math.min(200, narration.length())) + "...");

            // 3. Audio
            System.out.println("\n🎙️  === GÉNÉRATION AUDIO ===");
            String audioPath = generateAudioReliable(narration, langue);
            if (audioPath != null && new File(audioPath).exists()) {
                tour.setAudioPath(audioPath);
                tour.setDurationSeconds(estimateDuration(narration));
                System.out.println("✅ Audio : " + audioPath +
                        " (" + new File(audioPath).length() / 1024 + " KB)");
            } else {
                System.err.println("❌ Échec génération audio");
            }

            System.out.println("\n🎉 === GÉNÉRATION TERMINÉE ===\n");

        } catch (Exception e) {
            System.err.println("❌ ERREUR : " + e.getMessage());
            e.printStackTrace();
        }

        return tour;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AMÉLIORATION 1 : PHOTOS — Wikimedia Commons en priorité
    // ═══════════════════════════════════════════════════════════════════════════

    private List<String> fetchDiversePhotos(String lieu) throws Exception {
        List<String> allPhotos = new ArrayList<>();

        // ── Étape 1 : Wikimedia Commons (gratuit, images libres, très précises) ──
        System.out.println("🔍 [1/3] Recherche Wikimedia Commons...");
        try {
            List<String> wikiPhotos = fetchFromWikimedia(lieu, 6);
            allPhotos.addAll(wikiPhotos);
            System.out.println("  ✅ Wikimedia : " + wikiPhotos.size() + " photos");
        } catch (Exception e) {
            System.err.println("  ⚠️  Wikimedia échoué : " + e.getMessage());
        }

        // ── Étape 2 : Unsplash en complément si < 6 photos ──────────────────────
        if (allPhotos.size() < 6 && unsplashApiKey != null) {
            System.out.println("🔍 [2/3] Complément Unsplash...");
            for (String query : buildSearchQueries(lieu)) {
                try {
                    List<String> p = fetchFromUnsplash(query, 3);
                    allPhotos.addAll(p);
                    System.out.println("  ✅ Unsplash \"" + query + "\" : " + p.size());
                    if (allPhotos.size() >= 8) break;
                } catch (Exception e) {
                    System.err.println("  ⚠️  Unsplash \"" + query + "\" : " + e.getMessage());
                }
            }
        }

        // ── Étape 3 : Pexels en dernier recours ──────────────────────────────────
        if (allPhotos.size() < 6) {
            System.out.println("🔍 [3/3] Fallback Pexels...");
            try {
                List<String> p = fetchFromPexels(lieu, 8 - allPhotos.size());
                allPhotos.addAll(p);
                System.out.println("  ✅ Pexels : " + p.size() + " photos");
            } catch (Exception e) {
                System.err.println("  ⚠️  Pexels : " + e.getMessage());
            }
        }

        // Dédupliquer et limiter à 8
        Set<String> unique = new LinkedHashSet<>(allPhotos);
        List<String> result = new ArrayList<>(unique);
        return result.subList(0, Math.min(8, result.size()));
    }

    /**
     * Wikimedia Commons API — images libres de droits, très précises pour les sites historiques
     * Docs : https://commons.wikimedia.org/w/api.php
     */
    private List<String> fetchFromWikimedia(String lieu, int count) throws Exception {
        List<String> photoUrls = new ArrayList<>();

        // Recherche des fichiers images liés au lieu
        String encodedQuery = URLEncoder.encode(lieu + " tunisia", StandardCharsets.UTF_8);
        String apiUrl = "https://commons.wikimedia.org/w/api.php?" +
                "action=query" +
                "&generator=search" +
                "&gsrnamespace=6" +          // namespace 6 = fichiers images
                "&gsrsearch=" + encodedQuery +
                "&gsrlimit=" + count +
                "&prop=imageinfo" +
                "&iiprop=url|size" +
                "&iiurlwidth=1200" +          // largeur souhaitée
                "&format=json";

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "VirtualTourApp/1.0 (educational project)");
        conn.setConnectTimeout(10000);

        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);

        if (!json.has("query")) return photoUrls;

        JSONObject pages = json.getJSONObject("query").getJSONObject("pages");

        for (String key : pages.keySet()) {
            JSONObject page = pages.getJSONObject(key);
            if (!page.has("imageinfo")) continue;

            JSONArray imageInfoArray = page.getJSONArray("imageinfo");
            if (imageInfoArray.isEmpty()) continue;

            JSONObject imageInfo = imageInfoArray.getJSONObject(0);
            String imageUrl = imageInfo.optString("thumburl");
            if (imageUrl.isEmpty()) imageUrl = imageInfo.optString("url");

            // Filtrer les SVG et petites images
            int width = imageInfo.optInt("thumbwidth", 0);
            if (!imageUrl.isEmpty() && !imageUrl.endsWith(".svg") && width >= 400) {
                photoUrls.add(imageUrl);
                if (photoUrls.size() >= count) break;
            }
        }

        return photoUrls;
    }

    private String[] buildSearchQueries(String lieu) {
        List<String> queries = new ArrayList<>();
        queries.add(lieu + " tunisia");
        String l = lieu.toLowerCase();
        if (l.contains("sidi bou said"))  { queries.add("sidi bou said blue white"); queries.add("tunisian architecture"); }
        else if (l.contains("carthage")) { queries.add("carthage ruins tunisia"); queries.add("roman ruins africa"); }
        else if (l.contains("djerba"))   { queries.add("djerba island tunisia"); queries.add("tunisia beach mediterranean"); }
        else if (l.contains("kairouan")) { queries.add("kairouan mosque tunisia"); queries.add("islamic architecture tunisia"); }
        else { queries.add(lieu); queries.add(lieu + " landscape"); }
        return queries.toArray(new String[0]);
    }

    private List<String> fetchFromUnsplash(String query, int count) throws Exception {
        List<String> photoUrls = new ArrayList<>();
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String urlString = "https://api.unsplash.com/search/photos?query=" + encodedQuery +
                "&per_page=" + count + "&orientation=landscape&client_id=" + unsplashApiKey;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);

        if (conn.getResponseCode() == 200) {
            JSONObject json = new JSONObject(readResponse(conn));
            JSONArray results = json.getJSONArray("results");
            for (int i = 0; i < Math.min(results.length(), count); i++) {
                photoUrls.add(results.getJSONObject(i).getJSONObject("urls").getString("regular"));
            }
        }
        return photoUrls;
    }

    private List<String> fetchFromPexels(String lieu, int count) throws Exception {
        List<String> photoUrls = new ArrayList<>();
        String query = URLEncoder.encode(lieu + " tunisia", StandardCharsets.UTF_8);
        URL url = new URL("https://api.pexels.com/v1/search?query=" + query + "&per_page=" + count);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", PEXELS_API_KEY);

        if (conn.getResponseCode() == 200) {
            JSONArray photos = new JSONObject(readResponse(conn)).getJSONArray("photos");
            for (int i = 0; i < Math.min(photos.length(), count); i++) {
                photoUrls.add(photos.getJSONObject(i).getJSONObject("src").getString("large"));
            }
        }
        return photoUrls;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AMÉLIORATION 2 : NARRATION — Prompt enrichi + modèle plus puissant
    // ═══════════════════════════════════════════════════════════════════════════

    private String generateNarrationWithGroq(String lieu, String langue) throws Exception {

        // Récupérer le contexte culturel spécifique au lieu
        String context = getContextForLieu(lieu);
        String langueLabel = langue.equals("français") ? "français" : "arabe";

        // ── Prompt enrichi ─────────────────────────────────────────────────────
        // Avant : prompt générique de 2 lignes → narration plate
        // Après : contexte factuel + structure narrative + ton immersif
        String systemPrompt =
                "Tu es un guide touristique expert passionné, narrateur d'une visite virtuelle en vidéo. " +
                        "Ton style est vivant, sensoriel et immersif — tu fais voir, entendre et ressentir le lieu. " +
                        "Tu t'appuies sur des faits historiques et culturels précis pour captiver l'auditeur.";

        String userPrompt = String.format(
                "Génère une narration de visite virtuelle pour : **%s** (Tunisie)\n\n" +
                        "CONTEXTE CULTUREL ET HISTORIQUE À UTILISER :\n%s\n\n" +
                        "CONSIGNES STRICTES :\n" +
                        "- Langue : %s\n" +
                        "- Durée cible : 60-70 secondes à l'oral (environ 140-160 mots)\n" +
                        "- Structure en 4 temps :\n" +
                        "  1. Ouverture sensorielle et évocatrice (1-2 phrases)\n" +
                        "  2. Histoire et origine du lieu (2-3 phrases avec dates/faits précis)\n" +
                        "  3. Ce que le visiteur voit et ressent sur place (2-3 phrases descriptives)\n" +
                        "  4. Invitation finale mémorable (1 phrase)\n" +
                        "- Utilise des détails concrets du contexte fourni\n" +
                        "- PAS de titres, PAS de tirets, PAS de numérotation\n" +
                        "- Texte continu, fluide, conçu pour être lu à voix haute\n",
                lieu, context, langueLabel
        );

        URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + groqApiKey);
        conn.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        // Modèle upgradé : 70b >> 8b pour la qualité rédactionnelle
        requestBody.put("model", "llama-3.3-70b-versatile");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
        requestBody.put("messages", messages);

        requestBody.put("temperature", 0.75);   // légèrement créatif mais pas délirant
        requestBody.put("max_tokens", 400);

        OutputStream os = conn.getOutputStream();
        os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        os.close();

        if (conn.getResponseCode() == 200) {
            String response = readResponse(conn);
            return new JSONObject(response)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content").trim();
        }

        // Lire l'erreur pour debug
        BufferedReader errReader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder errBody = new StringBuilder();
        String line;
        while ((line = errReader.readLine()) != null) errBody.append(line);
        throw new Exception("Groq API " + conn.getResponseCode() + " : " + errBody);
    }

    /**
     * Résout le contexte culturel pour un lieu donné (insensible à la casse)
     */
    private String getContextForLieu(String lieu) {
        String lowerLieu = lieu.toLowerCase().trim();
        for (Map.Entry<String, String> entry : LIEU_CONTEXT.entrySet()) {
            if (lowerLieu.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return LIEU_CONTEXT.get("default") + " Lieu : " + lieu + ".";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AUDIO (inchangé — gTTS fonctionne bien)
    // ═══════════════════════════════════════════════════════════════════════════

    private String generateAudioReliable(String text, String langue) {
        String langCode = langue.equals("français") ? "fr" : "ar";
        String outputDir = "src/main/resources/audio/";
        String outputFile = "tour_" + System.currentTimeMillis() + ".mp3";
        String outputPath = outputDir + outputFile;

        try {
            Files.createDirectories(Paths.get(outputDir));
            String cleanText = text.replace("\"", "'").replace("\n", " ");
            String[] pythonCommands = {
                    "python", "python3", "py",
                    "C:\\Users\\hp\\AppData\\Local\\Programs\\Python\\Python313\\python.exe"
            };

            for (String pythonCmd : pythonCommands) {
                try {
                    System.out.println("🔧 Essai avec : " + pythonCmd);
                    ProcessBuilder pb = new ProcessBuilder(
                            pythonCmd, "-c",
                            String.format(
                                    "from gtts import gTTS; " +
                                            "tts = gTTS(text='''%s''', lang='%s', slow=False); " +
                                            "tts.save('%s'); print('OK')",
                                    cleanText.replace("'", "\\'"), langCode,
                                    outputPath.replace("\\", "\\\\")
                            )
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    // ✅ variable locale (non plus champ d'instance)
                    String line;
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    while ((line = reader.readLine()) != null) System.out.println("  " + line);

                    File audioFile = new File(outputPath);
                    if (process.waitFor() == 0 && audioFile.exists() && audioFile.length() > 1000) {
                        System.out.println("✅ Audio OK (" + pythonCmd + ")");
                        return outputPath;
                    }
                } catch (Exception e) {
                    System.err.println("  ❌ " + pythonCmd + " : " + e.getMessage());
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Erreur audio : " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════

    private String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();
        return response.toString();
    }

    private int estimateDuration(String text) {
        return (int) ((text.split("\\s+").length / 150.0) * 60);
    }

}