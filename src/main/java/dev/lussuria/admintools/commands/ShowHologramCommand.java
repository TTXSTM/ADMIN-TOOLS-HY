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
import com.hypixel.hytale.math.vector.Vector3f;
import dev.lussuria.admintools.AdminToolsPlugin;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.util.CommandInputUtil;
import dev.lussuria.admintools.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ShowHologramCommand extends AbstractCommand {
    private final AdminToolsPlugin plugin;
    private final AdminToolsConfig.ShowHologram config;

    public ShowHologramCommand(AdminToolsPlugin plugin, AdminToolsConfig.ShowHologram config) {
        super(config.name, config.description);
        this.plugin = plugin;
        this.config = config;
        if (config.aliases.length > 0) {
            addAliases(config.aliases);
        }
        if (config.permission != null && !config.permission.isBlank()) {
            requirePermission(config.permission);
        }
        setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        Player senderEntity = context.senderAs(Player.class);
        if (senderEntity == null) {
            context.sendMessage(Message.raw("Command can only be used by a player."));
            return CompletableFuture.completedFuture(null);
        }

        World world = senderEntity.getWorld();
        if (world == null) {
            context.sendMessage(Message.raw("Player is not in a world."));
            return CompletableFuture.completedFuture(null);
        }

        String[] args = CommandInputUtil.extractArgs(context, this);
        String senderName = context.sender().getDisplayName();

        world.execute(() -> {
            PlayerRef senderPlayer = senderEntity.getPlayerRef();
            PlayerRef targetPlayer = null;
            int textStartIndex = 0;

            if (args.length > 0) {
                targetPlayer = CommandInputUtil.findPlayerByName(world, args[0]);
                if (targetPlayer != null) {
                    textStartIndex = 1;
                }
            }
            if (targetPlayer == null) {
                targetPlayer = senderPlayer;
            }
            if (targetPlayer == null) {
                senderEntity.sendMessage(Message.raw("Player not found."));
                return;
            }

            Ref<EntityStore> targetRef = targetPlayer.getReference();
            if (targetRef == null || !targetRef.isValid()) {
                senderEntity.sendMessage(Message.raw("Player not found."));
                return;
            }

            Store<EntityStore> targetStore = targetRef.getStore();
            TransformComponent transform = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
            if (transform == null || transform.getPosition() == null) {
                senderEntity.sendMessage(Message.raw("Cannot read target position."));
                return;
            }

            Vector3d position = new Vector3d(transform.getPosition());
            position.add(0, config.heightOffset, 0);

            Vector3f rotation = new Vector3f(
                plugin.getConfig().hologram.defaultRotationYaw,
                plugin.getConfig().hologram.defaultRotationPitch,
                plugin.getConfig().hologram.defaultRotationRoll
            );

            String targetName = targetPlayer.getUsername();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("sender", senderName);
            placeholders.put("player", targetName);

            String customText = CommandInputUtil.join(args, textStartIndex).trim();
            String template = customText.isBlank() ? config.text : customText;
            String text = MessageUtil.applyPlaceholders(template, placeholders);

            plugin.spawnHologram(world, targetStore, position, rotation, text, config);
        });
        return CompletableFuture.completedFuture(null);
    }
}
