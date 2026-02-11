package dev.lussuria.admintools.role;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class RoleManager {
    private final HytaleLogger logger;
    private final Path dataFile;
    private final Map<UUID, ChatRole> roles = new ConcurrentHashMap<>();
    private final Map<String, UUID> rolesByName = new ConcurrentHashMap<>();

    public RoleManager(HytaleLogger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataFile = dataDirectory.resolve("roles.json");
    }

    // === CRUD ===

    public ChatRole createRole(String name, String displayName, String color, int priority) {
        ChatRole role = new ChatRole(name, displayName, color, priority);
        roles.put(role.getId(), role);
        rolesByName.put(name.toLowerCase(Locale.ROOT), role.getId());
        return role;
    }

    public boolean deleteRole(String name) {
        UUID id = rolesByName.remove(name.toLowerCase(Locale.ROOT));
        if (id == null) {
            return false;
        }
        roles.remove(id);
        return true;
    }

    public ChatRole getRole(String name) {
        UUID id = rolesByName.get(name.toLowerCase(Locale.ROOT));
        if (id == null) {
            return null;
        }
        return roles.get(id);
    }

    public Collection<ChatRole> getAllRoles() {
        return roles.values();
    }

    public boolean roleExists(String name) {
        return rolesByName.containsKey(name.toLowerCase(Locale.ROOT));
    }

    // === Role Resolution ===

    public ChatRole resolveRole(Set<String> playerGroups) {
        if (playerGroups == null || playerGroups.isEmpty()) {
            return null;
        }
        Set<String> upperGroups = new HashSet<>();
        for (String g : playerGroups) {
            if (g != null && !g.isBlank()) {
                upperGroups.add(g.toUpperCase(Locale.ROOT));
            }
        }
        ChatRole best = null;
        for (ChatRole role : roles.values()) {
            for (String roleGroup : role.getGroups()) {
                if (roleGroup != null && upperGroups.contains(roleGroup.toUpperCase(Locale.ROOT))) {
                    if (best == null || role.getPriority() < best.getPriority()) {
                        best = role;
                    }
                    break;
                }
            }
        }
        return best;
    }

    // === Persistence ===

    public void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(dataFile)) {
                writer.write("[\n");
                boolean first = true;
                for (ChatRole r : roles.values()) {
                    if (!first) {
                        writer.write(",\n");
                    }
                    first = false;
                    writer.write("  {\n");
                    writer.write("    \"Id\": \"" + r.getId() + "\",\n");
                    writer.write("    \"Name\": \"" + escapeJson(r.getName()) + "\",\n");
                    writer.write("    \"DisplayName\": \"" + escapeJson(r.getDisplayName()) + "\",\n");
                    writer.write("    \"Color\": \"" + escapeJson(r.getColor()) + "\",\n");
                    writer.write("    \"Bold\": " + r.isBold() + ",\n");
                    writer.write("    \"Italic\": " + r.isItalic() + ",\n");
                    writer.write("    \"Priority\": " + r.getPriority() + ",\n");
                    writer.write("    \"Groups\": [");
                    for (int i = 0; i < r.getGroups().size(); i++) {
                        if (i > 0) {
                            writer.write(", ");
                        }
                        writer.write("\"" + escapeJson(r.getGroups().get(i)) + "\"");
                    }
                    writer.write("]\n");
                    writer.write("  }");
                }
                writer.write("\n]\n");
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to save roles: %s", e.getMessage());
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
            parseRolesJson(content);
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to load roles: %s", e.getMessage());
        }
    }

    private void parseRolesJson(String json) {
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
                    parseRoleObject(objectJson);
                    objectStart = -1;
                }
            }
        }
    }

    private void parseRoleObject(String json) {
        try {
            ChatRole role = new ChatRole();
            String id = extractStringField(json, "Id");
            if (id != null && !id.isEmpty()) {
                role.setId(UUID.fromString(id));
            }
            String name = extractStringField(json, "Name");
            if (name != null) {
                role.setName(name);
            }
            String displayName = extractStringField(json, "DisplayName");
            if (displayName != null) {
                role.setDisplayName(displayName);
            }
            String color = extractStringField(json, "Color");
            if (color != null && !color.isEmpty()) {
                role.setColor(color);
            }
            role.setBold(extractBooleanField(json, "Bold"));
            role.setItalic(extractBooleanField(json, "Italic"));
            int priority = extractIntField(json, "Priority");
            if (priority > 0) {
                role.setPriority(priority);
            }
            List<String> groups = extractStringArray(json, "Groups");
            role.getGroups().addAll(groups);

            roles.put(role.getId(), role);
            rolesByName.put(role.getName().toLowerCase(Locale.ROOT), role.getId());
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to parse role entry: %s", e.getMessage());
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

    private int extractIntField(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return 0;
        }
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) {
            return 0;
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
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean extractBooleanField(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return false;
        }
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) {
            return false;
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
        return Boolean.parseBoolean(json.substring(start, end).trim());
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
