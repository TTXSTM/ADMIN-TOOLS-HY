package dev.lussuria.admintools.hologram;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import com.hypixel.hytale.logger.HytaleLogger;

public final class HologramManager {
    private final HytaleLogger logger;
    private final Path dataFile;
    private final float defaultScale;
    private final Map<UUID, HologramData> holograms = new ConcurrentHashMap<>();
    private final Map<String, UUID> hologramsByName = new ConcurrentHashMap<>();

    public HologramManager(HytaleLogger logger, Path dataDirectory, float defaultScale) {
        this.logger = logger;
        this.dataFile = dataDirectory.resolve("holograms.json");
        this.defaultScale = defaultScale;
    }

    // === CRUD ===

    public HologramData createHologram(String name, double x, double y, double z, String worldId, UUID creatorId) {
        HologramData hologram = new HologramData(name, x, y, z, worldId);
        hologram.setCreatorId(creatorId);
        holograms.put(hologram.getId(), hologram);
        hologramsByName.put(name.toLowerCase(Locale.ROOT), hologram.getId());
        return hologram;
    }

    public boolean deleteHologram(String name) {
        UUID id = hologramsByName.remove(name.toLowerCase(Locale.ROOT));
        if (id == null) {
            return false;
        }
        HologramData hologram = holograms.remove(id);
        if (hologram != null && hologram.isSpawned()) {
            despawnHologramEntities(hologram);
        }
        return true;
    }

    public HologramData getHologram(String name) {
        UUID id = hologramsByName.get(name.toLowerCase(Locale.ROOT));
        if (id == null) {
            return null;
        }
        return holograms.get(id);
    }

    public HologramData getHologram(UUID id) {
        return holograms.get(id);
    }

    public Collection<HologramData> getAllHolograms() {
        return holograms.values();
    }

    public boolean hologramExists(String name) {
        return hologramsByName.containsKey(name.toLowerCase(Locale.ROOT));
    }

    // === Line Operations ===

    public void addLine(HologramData hologram, String text) {
        hologram.getLines().add(text);
    }

    public void removeLine(HologramData hologram, int lineIndex) {
        if (lineIndex >= 0 && lineIndex < hologram.getLines().size()) {
            hologram.getLines().remove(lineIndex);
        }
    }

    public void setLine(HologramData hologram, int lineIndex, String text) {
        if (lineIndex >= 0 && lineIndex < hologram.getLines().size()) {
            hologram.getLines().set(lineIndex, text);
        }
    }

    // === Spawn/Despawn ===

    public void spawnHologram(HologramData hologram) {
        World world = findWorld(hologram.getWorldId());
        if (world == null) {
            return;
        }
        world.execute(() -> spawnHologramEntities(world, hologram));
    }

    public void despawnHologram(HologramData hologram) {
        World world = findWorld(hologram.getWorldId());
        if (world == null) {
            despawnHologramEntities(hologram);
            return;
        }
        world.execute(() -> despawnHologramEntities(hologram));
    }

    public void respawnHologram(HologramData hologram) {
        World world = findWorld(hologram.getWorldId());
        if (world == null) {
            return;
        }
        world.execute(() -> {
            despawnHologramEntities(hologram);
            spawnHologramEntities(world, hologram);
        });
    }

    public void moveHologram(HologramData hologram, double x, double y, double z) {
        hologram.setPosition(x, y, z);
        respawnHologram(hologram);
    }

    public void spawnAllHolograms() {
        for (HologramData hologram : holograms.values()) {
            if (!hologram.isSpawned()) {
                spawnHologram(hologram);
            }
        }
    }

    public void despawnAllHolograms() {
        for (HologramData hologram : holograms.values()) {
            if (hologram.isSpawned()) {
                despawnHologramEntities(hologram);
            }
        }
    }

    private void spawnHologramEntities(World world, HologramData hologram) {
        hologram.clearLineEntityRefs();
        logger.at(Level.INFO).log("[HoloMgr] Spawning hologram '%s' with %d lines at (%.1f, %.1f, %.1f), scale=%.4f",
            hologram.getName(), hologram.getLines().size(), hologram.getPosX(), hologram.getPosY(), hologram.getPosZ(), defaultScale);
        for (int i = 0; i < hologram.getLines().size(); i++) {
            String lineText = hologram.getLines().get(i);
            double lineY = hologram.getLinePositionY(i);
            Vector3d pos = new Vector3d(hologram.getPosX(), lineY, hologram.getPosZ());
            Ref<EntityStore> ref = HologramSpawner.spawnLine(world, pos, lineText, defaultScale);
            if (ref != null && ref.isValid()) {
                hologram.getLineEntityRefs().add(ref);
                logger.at(Level.INFO).log("[HoloMgr] Spawned line %d: '%s' at y=%.1f, ref valid=%b", i, lineText, lineY, ref.isValid());
            } else {
                logger.at(Level.WARNING).log("[HoloMgr] Failed to spawn line %d: '%s'", i, lineText);
            }
        }
        hologram.setSpawned(true);
    }

    private void despawnHologramEntities(HologramData hologram) {
        for (Ref<EntityStore> ref : hologram.getLineEntityRefs()) {
            HologramSpawner.removeLine(ref);
        }
        hologram.clearLineEntityRefs();
        hologram.setSpawned(false);
    }

    private World findWorld(String worldId) {
        if (worldId == null || worldId.isEmpty()) {
            return null;
        }
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            for (World world : universe.getWorlds().values()) {
                if (worldId.equals(world.getName())) {
                    return world;
                }
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to find world: %s - %s", worldId, e.getMessage());
        }
        return null;
    }

    // === Persistence ===

    public void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(dataFile)) {
                writer.write("[\n");
                boolean first = true;
                for (HologramData h : holograms.values()) {
                    if (!first) {
                        writer.write(",\n");
                    }
                    first = false;
                    writer.write("  {\n");
                    writer.write("    \"Id\": \"" + h.getId() + "\",\n");
                    writer.write("    \"Name\": \"" + escapeJson(h.getName()) + "\",\n");
                    writer.write("    \"PosX\": " + h.getPosX() + ",\n");
                    writer.write("    \"PosY\": " + h.getPosY() + ",\n");
                    writer.write("    \"PosZ\": " + h.getPosZ() + ",\n");
                    writer.write("    \"WorldId\": \"" + escapeJson(h.getWorldId() == null ? "" : h.getWorldId()) + "\",\n");
                    writer.write("    \"CreatorId\": \"" + (h.getCreatorId() == null ? "" : h.getCreatorId()) + "\",\n");
                    writer.write("    \"LineSpacing\": " + h.getLineSpacing() + ",\n");
                    writer.write("    \"Lines\": [");
                    for (int i = 0; i < h.getLines().size(); i++) {
                        if (i > 0) {
                            writer.write(", ");
                        }
                        writer.write("\"" + escapeJson(h.getLines().get(i)) + "\"");
                    }
                    writer.write("]\n");
                    writer.write("  }");
                }
                writer.write("\n]\n");
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to save holograms: %s", e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(dataFile)) {
            return;
        }
        try {
            String content = Files.readString(dataFile).trim();
            if (content.isEmpty() || content.equals("[]")) {
                return;
            }
            parseHologramsJson(content);
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to load holograms: %s", e.getMessage());
        }
    }

    private void parseHologramsJson(String json) {
        int depth = 0;
        int objectStart = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    String objectJson = json.substring(objectStart, i + 1);
                    parseHologramObject(objectJson);
                    objectStart = -1;
                }
            }
        }
    }

    private void parseHologramObject(String json) {
        try {
            HologramData hologram = new HologramData();
            String id = extractStringField(json, "Id");
            if (id != null && !id.isEmpty()) {
                hologram.setId(UUID.fromString(id));
            }
            String name = extractStringField(json, "Name");
            if (name != null) {
                hologram.setName(name);
            }
            hologram.setPosition(
                extractDoubleField(json, "PosX"),
                extractDoubleField(json, "PosY"),
                extractDoubleField(json, "PosZ")
            );
            hologram.setWorldId(extractStringField(json, "WorldId"));
            String creatorId = extractStringField(json, "CreatorId");
            if (creatorId != null && !creatorId.isEmpty()) {
                hologram.setCreatorId(UUID.fromString(creatorId));
            }
            double spacing = extractDoubleField(json, "LineSpacing");
            if (spacing > 0) {
                hologram.setLineSpacing(spacing);
            }
            List<String> lines = extractStringArray(json, "Lines");
            hologram.getLines().addAll(lines);

            holograms.put(hologram.getId(), hologram);
            hologramsByName.put(hologram.getName().toLowerCase(Locale.ROOT), hologram.getId());
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to parse hologram entry: %s", e.getMessage());
        }
    }

    private String extractStringField(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return null;
        }
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) {
            return null;
        }
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) {
            return null;
        }
        int quoteEnd = findClosingQuote(json, quoteStart + 1);
        if (quoteEnd < 0) {
            return null;
        }
        return unescapeJson(json.substring(quoteStart + 1, quoteEnd));
    }

    private double extractDoubleField(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return 0.0;
        }
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) {
            return 0.0;
        }
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || c == '\n') {
                break;
            }
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return result;
        }
        int bracketStart = json.indexOf('[', idx + pattern.length());
        if (bracketStart < 0) {
            return result;
        }
        int bracketEnd = json.indexOf(']', bracketStart);
        if (bracketEnd < 0) {
            return result;
        }
        String arrayContent = json.substring(bracketStart + 1, bracketEnd);
        int pos = 0;
        while (pos < arrayContent.length()) {
            int quoteStart = arrayContent.indexOf('"', pos);
            if (quoteStart < 0) {
                break;
            }
            int quoteEnd = findClosingQuote(arrayContent, quoteStart + 1);
            if (quoteEnd < 0) {
                break;
            }
            result.add(unescapeJson(arrayContent.substring(quoteStart + 1, quoteEnd)));
            pos = quoteEnd + 1;
        }
        return result;
    }

    private int findClosingQuote(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '\\') {
                i++;
                continue;
            }
            if (str.charAt(i) == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
    }

    private static String unescapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\\"", "\"")
                     .replace("\\n", "\n")
                     .replace("\\r", "\r")
                     .replace("\\t", "\t")
                     .replace("\\\\", "\\");
    }
}
