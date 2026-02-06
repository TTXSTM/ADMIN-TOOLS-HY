package dev.lussuria.admintools.util;

import com.hypixel.hytale.server.core.Message;

import java.util.Map;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static Message renderMessage(String template, Map<String, String> placeholders, boolean parse) {
        String text = applyPlaceholders(template, placeholders);
        if (!parse) {
            return Message.raw(text);
        }
        try {
            return Message.parse(text);
        } catch (Exception ex) {
            return Message.raw(text);
        }
    }

    public static String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty() || placeholders == null || placeholders.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace(key, value);
        }
        return result;
    }
}
