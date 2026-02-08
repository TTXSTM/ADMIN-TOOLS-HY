package dev.lussuria.admintools.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import dev.lussuria.admintools.AdminToolsPlugin;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.hologram.HologramData;
import dev.lussuria.admintools.hologram.HologramManager;
import dev.lussuria.admintools.ui.HologramEditorPage;
import dev.lussuria.admintools.util.CommandInputUtil;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public final class HologramCommand extends AbstractCommand {
    private final AdminToolsPlugin plugin;

    public HologramCommand(AdminToolsPlugin plugin, AdminToolsConfig.HologramCommands config) {
        super(config.name, config.description);
        this.plugin = plugin;
        if (config.aliases.length > 0) {
            addAliases(config.aliases);
        }
        if (config.permission != null && !config.permission.isBlank()) {
            requirePermission(config.permission);
        }
        setAllowsExtraArguments(true);

        addSubCommand(new CreateSubCommand(plugin, config));
        addSubCommand(new DeleteSubCommand(plugin));
        addSubCommand(new EditSubCommand(plugin));
        addSubCommand(new ListSubCommand(plugin));
        addSubCommand(new AddLineSubCommand(plugin));
        addSubCommand(new RemoveLineSubCommand(plugin));
        addSubCommand(new MoveHereSubCommand(plugin));
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        context.sendMessage(Message.raw("Usage: /holo <create|edit|delete|list|addline|removeline|movehere>"));
        return CompletableFuture.completedFuture(null);
    }

    static final class CreateSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;
        private final AdminToolsConfig.HologramCommands config;

        CreateSubCommand(AdminToolsPlugin plugin, AdminToolsConfig.HologramCommands config) {
            super("create", "Create a new hologram.");
            this.plugin = plugin;
            this.config = config;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            Player senderEntity = context.senderAs(Player.class);
            if (senderEntity == null) {
                context.sendMessage(Message.raw("Command can only be used by a player."));
                return CompletableFuture.completedFuture(null);
            }

            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 1) {
                context.sendMessage(Message.raw("Usage: /holo create <name>"));
                return CompletableFuture.completedFuture(null);
            }

            String name = args[0];
            World world = senderEntity.getWorld();
            if (world == null) {
                context.sendMessage(Message.raw("Player is not in a world."));
                return CompletableFuture.completedFuture(null);
            }

            world.execute(() -> {
                HologramManager manager = plugin.getHologramManager();
                if (manager.hologramExists(name)) {
                    senderEntity.sendMessage(Message.raw("Hologram '" + name + "' already exists."));
                    return;
                }

                PlayerRef playerRef = senderEntity.getPlayerRef();
                Ref<EntityStore> playerEntityRef = playerRef.getReference();
                Store<EntityStore> store = playerEntityRef.getStore();
                TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                if (transform == null || transform.getPosition() == null) {
                    senderEntity.sendMessage(Message.raw("Cannot read position."));
                    return;
                }

                Vector3d position = new Vector3d(transform.getPosition());
                HologramData hologram = manager.createHologram(
                    name,
                    position.getX(), position.getY(), position.getZ(),
                    world.getName(),
                    playerRef.getUuid()
                );
                hologram.getLines().add("New Hologram: " + name);
                hologram.getLines().add("Use /holo edit " + name);
                hologram.setLineSpacing(config.defaultLineSpacing);
                manager.spawnHologram(hologram);
                manager.save();

                senderEntity.sendMessage(Message.raw("Hologram '" + name + "' created."));

                Player player = store.getComponent(playerEntityRef, Player.getComponentType());
                if (player != null) {
                    player.getPageManager().openCustomPage(
                        playerEntityRef, store,
                        new HologramEditorPage(playerRef, plugin, hologram)
                    );
                }
            });
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class DeleteSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        DeleteSubCommand(AdminToolsPlugin plugin) {
            super("delete", "Delete a hologram.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 1) {
                context.sendMessage(Message.raw("Usage: /holo delete <name>"));
                return CompletableFuture.completedFuture(null);
            }

            String name = args[0];
            HologramManager manager = plugin.getHologramManager();
            if (manager.deleteHologram(name)) {
                manager.save();
                context.sendMessage(Message.raw("Hologram '" + name + "' deleted."));
            } else {
                context.sendMessage(Message.raw("Hologram '" + name + "' not found."));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class EditSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        EditSubCommand(AdminToolsPlugin plugin) {
            super("edit", "Edit a hologram.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            Player senderEntity = context.senderAs(Player.class);
            if (senderEntity == null) {
                context.sendMessage(Message.raw("Command can only be used by a player."));
                return CompletableFuture.completedFuture(null);
            }

            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 1) {
                context.sendMessage(Message.raw("Usage: /holo edit <name>"));
                return CompletableFuture.completedFuture(null);
            }

            String name = args[0];
            HologramManager manager = plugin.getHologramManager();
            HologramData hologram = manager.getHologram(name);
            if (hologram == null) {
                context.sendMessage(Message.raw("Hologram '" + name + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            World world = senderEntity.getWorld();
            if (world == null) {
                return CompletableFuture.completedFuture(null);
            }

            world.execute(() -> {
                PlayerRef playerRef = senderEntity.getPlayerRef();
                Ref<EntityStore> playerEntityRef = playerRef.getReference();
                Store<EntityStore> store = playerEntityRef.getStore();
                Player player = store.getComponent(playerEntityRef, Player.getComponentType());
                if (player != null) {
                    player.getPageManager().openCustomPage(
                        playerEntityRef, store,
                        new HologramEditorPage(playerRef, plugin, hologram)
                    );
                }
            });
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class ListSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        ListSubCommand(AdminToolsPlugin plugin) {
            super("list", "List all holograms.");
            this.plugin = plugin;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            HologramManager manager = plugin.getHologramManager();
            Collection<HologramData> all = manager.getAllHolograms();
            if (all.isEmpty()) {
                context.sendMessage(Message.raw("No holograms found."));
                return CompletableFuture.completedFuture(null);
            }

            StringBuilder sb = new StringBuilder("Holograms (" + all.size() + "):\n");
            for (HologramData h : all) {
                sb.append("  - ").append(h.getName())
                  .append(" at (")
                  .append(String.format("%.1f", h.getPosX())).append(", ")
                  .append(String.format("%.1f", h.getPosY())).append(", ")
                  .append(String.format("%.1f", h.getPosZ())).append(")")
                  .append(" [").append(h.getLines().size()).append(" lines]")
                  .append("\n");
            }
            context.sendMessage(Message.raw(sb.toString().trim()));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class AddLineSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        AddLineSubCommand(AdminToolsPlugin plugin) {
            super("addline", "Add a line to a hologram.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /holo addline <name> <text>"));
                return CompletableFuture.completedFuture(null);
            }

            String name = args[0];
            String text = CommandInputUtil.join(args, 1).trim();
            HologramManager manager = plugin.getHologramManager();
            HologramData hologram = manager.getHologram(name);
            if (hologram == null) {
                context.sendMessage(Message.raw("Hologram '" + name + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            manager.addLine(hologram, text);
            manager.respawnHologram(hologram);
            manager.save();
            context.sendMessage(Message.raw("Line added to '" + name + "'."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class RemoveLineSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        RemoveLineSubCommand(AdminToolsPlugin plugin) {
            super("removeline", "Remove a line from a hologram.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 2) {
                context.sendMessage(Message.raw("Usage: /holo removeline <name> <index>"));
                return CompletableFuture.completedFuture(null);
            }

            String name = args[0];
            int index;
            try {
                index = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException e) {
                context.sendMessage(Message.raw("Invalid line number."));
                return CompletableFuture.completedFuture(null);
            }

            HologramManager manager = plugin.getHologramManager();
            HologramData hologram = manager.getHologram(name);
            if (hologram == null) {
                context.sendMessage(Message.raw("Hologram '" + name + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            if (index < 0 || index >= hologram.getLines().size()) {
                context.sendMessage(Message.raw("Line index out of range (1-" + hologram.getLines().size() + ")."));
                return CompletableFuture.completedFuture(null);
            }

            manager.removeLine(hologram, index);
            manager.respawnHologram(hologram);
            manager.save();
            context.sendMessage(Message.raw("Line " + (index + 1) + " removed from '" + name + "'."));
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class MoveHereSubCommand extends AbstractCommand {
        private final AdminToolsPlugin plugin;

        MoveHereSubCommand(AdminToolsPlugin plugin) {
            super("movehere", "Move a hologram to your position.");
            this.plugin = plugin;
            setAllowsExtraArguments(true);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            Player senderEntity = context.senderAs(Player.class);
            if (senderEntity == null) {
                context.sendMessage(Message.raw("Command can only be used by a player."));
                return CompletableFuture.completedFuture(null);
            }

            String[] args = CommandInputUtil.extractArgs(context, this);
            if (args.length < 1) {
                context.sendMessage(Message.raw("Usage: /holo movehere <name>"));
                return CompletableFuture.completedFuture(null);
            }

            String name = args[0];
            HologramManager manager = plugin.getHologramManager();
            HologramData hologram = manager.getHologram(name);
            if (hologram == null) {
                context.sendMessage(Message.raw("Hologram '" + name + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            World world = senderEntity.getWorld();
            if (world == null) {
                return CompletableFuture.completedFuture(null);
            }

            world.execute(() -> {
                PlayerRef playerRef = senderEntity.getPlayerRef();
                Ref<EntityStore> playerEntityRef = playerRef.getReference();
                Store<EntityStore> store = playerEntityRef.getStore();
                TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
                if (transform == null || transform.getPosition() == null) {
                    senderEntity.sendMessage(Message.raw("Cannot read position."));
                    return;
                }

                Vector3d position = new Vector3d(transform.getPosition());
                manager.moveHologram(hologram, position.getX(), position.getY(), position.getZ());
                manager.save();
                senderEntity.sendMessage(Message.raw("Hologram '" + name + "' moved."));
            });
            return CompletableFuture.completedFuture(null);
        }
    }
}
