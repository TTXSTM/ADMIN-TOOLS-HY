package dev.lussuria.admintools.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import dev.lussuria.admintools.AdminToolsPlugin;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.role.ChatRole;
import dev.lussuria.admintools.role.RoleManager;
import dev.lussuria.admintools.util.CommandInputUtil;
import dev.lussuria.admintools.util.MessageUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.UUID;

public final class RoleCommand extends AbstractCommand {
    private static final Pattern HEX_COLOR = Pattern.compile("#?[0-9a-fA-F]{6}");
    private final AdminToolsPlugin plugin;

    public RoleCommand(AdminToolsPlugin plugin, AdminToolsConfig.RoleCommands config) {
        super(config.name, config.description);
        this.plugin = plugin;
        if (config.aliases.length > 0) {
            addAliases(config.aliases);
        }
        if (config.permission != null && !config.permission.isBlank()) {
            requirePermission(config.permission);
        }
        setAllowsExtraArguments(true);

        addSubCommand(new CreateSubCommand(plugin));
        addSubCommand(new DeleteSubCommand(plugin));
        addSubCommand(new ListSubCommand(plugin));
        addSubCommand(new InfoSubCommand(plugin));
        addSubCommand(new SetColorSubCommand(plugin));
        addSubCommand(new SetPrefixSubCommand(plugin));
        addSubCommand(new AddGroupSubCommand(plugin));
        addSubCommand(new RemoveGroupSubCommand(plugin));
        addSubCommand(new SetPrioritySubCommand(plugin));
        addSubCommand(new SetBoldSubCommand(plugin));
        addSubCommand(new SetItalicSubCommand(plugin));
        addSubCommand(new AssignSubCommand(plugin));
        addSubCommand(new UnassignSubCommand(plugin));
        addSubCommand(new AssignedSubCommand(plugin));
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        context.sendMessage(Message.raw(
            "Usage: /role <create|delete|list|info|setcolor|setprefix|addgroup|removegroup|setpriority|setbold|setitalic|assign|unassign|assigned>"
        ));
        return CompletableFuture.completedFuture(null);
    }

    static String normalizeHexColor(String input) {
        if (input == null) {
            return null;
        }
        String color = input.trim();
        if (!color.startsWith("#")) {
            color = "#" + color;
        }
        if (!HEX_COLOR.matcher(color).matches()) {
            return null;
        }
        return color.toLowerCase(Locale.ROOT);
    }

    private static UUID resolvePlayerUuid(String playerToken) {
        if (playerToken == null || playerToken.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(playerToken);
        } catch (IllegalArgumentException ignored) {
            PlayerRef ref = CommandInputUtil.findOnlinePlayerByName(playerToken);
            return ref == null ? null : ref.getUuid();
        }
    }

    private static String resolvePlayerName(UUID playerUuid, String fallback) {
        PlayerRef ref = CommandInputUtil.findOnlinePlayerByUuid(playerUuid);
        if (ref != null && ref.getUsername() != null && !ref.getUsername().isBlank()) {
            return ref.getUsername();
        }
        return fallback;
    }

    // === Subcommands ===

    static final class CreateSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        CreateSubCommand(AdminToolsPlugin plugin) {
            super("create", "Create a new chat role.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role create <name> <displayName...> [color] [priority]"));
                return CompletableFuture.completedFuture(null);
            }

            String name = args[0];
            String color = "#ffffff";
            int priority = 100;
            int displayEnd = args.length;

            // Parse optional args from the end so display name can contain spaces
            if (displayEnd > 2) {
                try {
                    priority = Integer.parseInt(args[displayEnd - 1]);
                    displayEnd--;
                } catch (NumberFormatException ignored) {
                }
            }
            if (displayEnd > 2) {
                String normalized = normalizeHexColor(args[displayEnd - 1]);
                if (normalized != null) {
                    color = normalized;
                    displayEnd--;
                }
            }

            String displayName = CommandInputUtil.join(args, 1, displayEnd);

            RoleManager manager = plugin.getRoleManager();
            if (manager.roleExists(name)) {
                context.sendMessage(Message.raw("Role '" + name + "' already exists."));
                return CompletableFuture.completedFuture(null);
            }

            manager.createRole(name, displayName, color, priority);
            manager.save();
            context.sendMessage(Message.raw("Role '" + name + "' created with prefix " + displayName + "."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class DeleteSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        DeleteSubCommand(AdminToolsPlugin plugin) {
            super("delete", "Delete a chat role.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 1) {
                context.sendMessage(Message.raw("Usage: /role delete <name>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            if (manager.deleteRole(args[0])) {
                manager.save();
                context.sendMessage(Message.raw("Role '" + args[0] + "' deleted."));
            } else {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class ListSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        ListSubCommand(AdminToolsPlugin plugin) {
            super("list", "List all chat roles.");
            this.plugin = plugin;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            RoleManager manager = plugin.getRoleManager();
            Collection<ChatRole> all = manager.getAllRoles();
            if (all.isEmpty()) {
                context.sendMessage(Message.raw("No roles defined."));
                return CompletableFuture.completedFuture(null);
            }

            List<ChatRole> sorted = new ArrayList<>(all);
            sorted.sort(Comparator.comparingInt(ChatRole::getPriority));

            Message result = Message.raw("Roles (" + sorted.size() + "):\n");
            for (ChatRole role : sorted) {
                Message preview = MessageUtil.buildRolePreview(role);
                result.insert(Message.raw("  - " + role.getName() + ": "));
                result.insert(preview);
                result.insert(Message.raw(" " + role.getColor()
                    + " priority=" + role.getPriority()
                    + " groups=" + role.getGroups() + "\n"));
            }
            context.sendMessage(result);
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class InfoSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        InfoSubCommand(AdminToolsPlugin plugin) {
            super("info", "Show details of a chat role.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 1) {
                context.sendMessage(Message.raw("Usage: /role info <name>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getRole(args[0]);
            if (role == null) {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            Message result = Message.raw("Role: " + role.getName() + "\n");
            result.insert(Message.raw("  Display: "));
            result.insert(MessageUtil.buildRolePreview(role));
            result.insert(Message.raw("\n  Color: " + role.getColor()
                + "\n  Bold: " + role.isBold()
                + "\n  Italic: " + role.isItalic()
                + "\n  Priority: " + role.getPriority()
                + "\n  Groups: " + role.getGroups()));
            context.sendMessage(result);
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class SetColorSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        SetColorSubCommand(AdminToolsPlugin plugin) {
            super("setcolor", "Set a role's color.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role setcolor <name> <#hexColor>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getRole(args[0]);
            if (role == null) {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            String color = normalizeHexColor(args[1]);
            if (color == null) {
                context.sendMessage(Message.raw("Invalid hex color. Use format: #ff5555"));
                return CompletableFuture.completedFuture(null);
            }

            role.setColor(color);
            manager.save();
            context.sendMessage(Message.raw("Role '" + args[0] + "' color set to " + color + "."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class SetPrefixSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        SetPrefixSubCommand(AdminToolsPlugin plugin) {
            super("setprefix", "Set a role's display name.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role setprefix <name> <displayName...>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getRole(args[0]);
            if (role == null) {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            String displayName = CommandInputUtil.join(args, 1).trim();
            role.setDisplayName(displayName);
            manager.save();
            context.sendMessage(Message.raw("Role '" + args[0] + "' prefix set to " + displayName + "."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class AddGroupSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        AddGroupSubCommand(AdminToolsPlugin plugin) {
            super("addgroup", "Add a permission group to a role.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role addgroup <name> <groupName>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getRole(args[0]);
            if (role == null) {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            String group = args[1];
            for (String existing : role.getGroups()) {
                if (existing.equalsIgnoreCase(group)) {
                    context.sendMessage(Message.raw("Group '" + group + "' is already in role '" + args[0] + "'."));
                    return CompletableFuture.completedFuture(null);
                }
            }

            role.getGroups().add(group);
            manager.save();
            context.sendMessage(Message.raw("Group '" + group + "' added to role '" + args[0] + "'."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class RemoveGroupSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        RemoveGroupSubCommand(AdminToolsPlugin plugin) {
            super("removegroup", "Remove a permission group from a role.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role removegroup <name> <groupName>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getRole(args[0]);
            if (role == null) {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            String group = args[1];
            boolean removed = role.getGroups().removeIf(g -> g.equalsIgnoreCase(group));
            if (removed) {
                manager.save();
                context.sendMessage(Message.raw("Group '" + group + "' removed from role '" + args[0] + "'."));
            } else {
                context.sendMessage(Message.raw("Group '" + group + "' not found in role '" + args[0] + "'."));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class SetPrioritySubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        SetPrioritySubCommand(AdminToolsPlugin plugin) {
            super("setpriority", "Set a role's priority.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role setpriority <name> <number>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getRole(args[0]);
            if (role == null) {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            int priority;
            try {
                priority = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                context.sendMessage(Message.raw("Invalid number."));
                return CompletableFuture.completedFuture(null);
            }

            role.setPriority(priority);
            manager.save();
            context.sendMessage(Message.raw("Role '" + args[0] + "' priority set to " + priority + "."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class SetBoldSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        SetBoldSubCommand(AdminToolsPlugin plugin) {
            super("setbold", "Set a role's bold style.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role setbold <name> <true|false>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getRole(args[0]);
            if (role == null) {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            boolean bold = Boolean.parseBoolean(args[1]);
            role.setBold(bold);
            manager.save();
            context.sendMessage(Message.raw("Role '" + args[0] + "' bold set to " + bold + "."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class SetItalicSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        SetItalicSubCommand(AdminToolsPlugin plugin) {
            super("setitalic", "Set a role's italic style.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role setitalic <name> <true|false>"));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getRole(args[0]);
            if (role == null) {
                context.sendMessage(Message.raw("Role '" + args[0] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            boolean italic = Boolean.parseBoolean(args[1]);
            role.setItalic(italic);
            manager.save();
            context.sendMessage(Message.raw("Role '" + args[0] + "' italic set to " + italic + "."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class AssignSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        AssignSubCommand(AdminToolsPlugin plugin) {
            super("assign", "Assign a role to a player.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /role assign <playerName|playerUuid> <roleName>"));
                return CompletableFuture.completedFuture(null);
            }

            UUID playerUuid = resolvePlayerUuid(args[0]);
            if (playerUuid == null) {
                context.sendMessage(Message.raw("Player '" + args[0] + "' not found online. Use UUID for offline player."));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            if (!manager.assignRole(playerUuid, args[1])) {
                context.sendMessage(Message.raw("Role '" + args[1] + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            manager.save();
            String displayName = resolvePlayerName(playerUuid, args[0]);
            context.sendMessage(Message.raw("Assigned role '" + args[1] + "' to " + displayName + "."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class UnassignSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        UnassignSubCommand(AdminToolsPlugin plugin) {
            super("unassign", "Remove assigned role from a player.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 1) {
                context.sendMessage(Message.raw("Usage: /role unassign <playerName|playerUuid>"));
                return CompletableFuture.completedFuture(null);
            }

            UUID playerUuid = resolvePlayerUuid(args[0]);
            if (playerUuid == null) {
                context.sendMessage(Message.raw("Player '" + args[0] + "' not found online. Use UUID for offline player."));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            if (!manager.unassignRole(playerUuid)) {
                context.sendMessage(Message.raw("Player has no assigned role."));
                return CompletableFuture.completedFuture(null);
            }

            manager.save();
            String displayName = resolvePlayerName(playerUuid, args[0]);
            context.sendMessage(Message.raw("Removed assigned role from " + displayName + "."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class AssignedSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        AssignedSubCommand(AdminToolsPlugin plugin) {
            super("assigned", "Show assigned role of a player.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 1) {
                context.sendMessage(Message.raw("Usage: /role assigned <playerName|playerUuid>"));
                return CompletableFuture.completedFuture(null);
            }

            UUID playerUuid = resolvePlayerUuid(args[0]);
            if (playerUuid == null) {
                context.sendMessage(Message.raw("Player '" + args[0] + "' not found online. Use UUID for offline player."));
                return CompletableFuture.completedFuture(null);
            }

            RoleManager manager = plugin.getRoleManager();
            ChatRole role = manager.getAssignedRole(playerUuid);
            if (role == null) {
                context.sendMessage(Message.raw("No assigned role for this player."));
                return CompletableFuture.completedFuture(null);
            }

            String displayName = resolvePlayerName(playerUuid, args[0]);
            Message result = Message.raw("Assigned role for " + displayName + ": ");
            result.insert(MessageUtil.buildRolePreview(role));
            result.insert(Message.raw(" (" + role.getName() + ")"));
            context.sendMessage(result);
            return CompletableFuture.completedFuture(null);
        }
    }
}
