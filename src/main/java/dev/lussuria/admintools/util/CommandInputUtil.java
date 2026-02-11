package dev.lussuria.admintools.util;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class CommandInputUtil {
    private CommandInputUtil() {
    }

    public static String[] extractArgs(CommandContext context, AbstractCommand command) {
        if (context == null || command == null) {
            return new String[0];
        }
        String input = context.getInputString();
        if (input == null) {
            return new String[0];
        }
        input = input.trim();
        if (input.startsWith("/")) {
            input = input.substring(1).trim();
        }
        if (input.isEmpty()) {
            return new String[0];
        }
        String[] tokens = input.split("\\s+");
        Set<String> names = new HashSet<>();
        if (command.getName() != null) {
            names.add(command.getName().toLowerCase(Locale.ROOT));
        }
        for (String alias : command.getAliases()) {
            if (alias != null && !alias.isBlank()) {
                names.add(alias.toLowerCase(Locale.ROOT));
            }
        }
        int index = -1;
        for (int i = 0; i < tokens.length; i++) {
            if (names.contains(tokens[i].toLowerCase(Locale.ROOT))) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return tokens;
        }
        if (index + 1 >= tokens.length) {
            return new String[0];
        }
        return Arrays.copyOfRange(tokens, index + 1, tokens.length);
    }

    public static String join(String[] args, int startIndex) {
        return join(args, startIndex, args == null ? 0 : args.length);
    }

    public static String join(String[] args, int startIndex, int endIndex) {
        if (args == null || args.length == 0 || startIndex >= endIndex) {
            return "";
        }
        int end = Math.min(endIndex, args.length);
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < end; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    public static PlayerRef findPlayerByName(World world, String name) {
        if (world == null || name == null || name.isBlank()) {
            return null;
        }
        for (PlayerRef ref : world.getPlayerRefs()) {
            if (ref != null && name.equalsIgnoreCase(ref.getUsername())) {
                return ref;
            }
        }
        return null;
    }

    public static PlayerRef findOnlinePlayerByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Universe universe = Universe.get();
        if (universe == null || universe.getWorlds() == null) {
            return null;
        }
        for (World world : universe.getWorlds().values()) {
            if (world == null) {
                continue;
            }
            PlayerRef ref = findPlayerByName(world, name);
            if (ref != null) {
                return ref;
            }
        }
        return null;
    }

    public static PlayerRef findOnlinePlayerByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Universe universe = Universe.get();
        if (universe == null || universe.getWorlds() == null) {
            return null;
        }
        for (World world : universe.getWorlds().values()) {
            if (world == null) {
                continue;
            }
            for (PlayerRef ref : world.getPlayerRefs()) {
                if (ref != null && uuid.equals(ref.getUuid())) {
                    return ref;
                }
            }
        }
        return null;
    }
}
