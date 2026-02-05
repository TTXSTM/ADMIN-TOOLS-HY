package dev.lussuria.admintools.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import dev.lussuria.admintools.AdminToolsPlugin;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;

public final class ShowHologramCommand extends AbstractTargetPlayerCommand {
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
    }

    @Override
    protected void execute(
        CommandContext context,
        Ref<EntityStore> senderRef,
        Ref<EntityStore> targetRef,
        PlayerRef targetPlayer,
        World world,
        Store<EntityStore> store
    ) {
        TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            context.sendMessage(com.hypixel.hytale.server.core.Message.raw("Cannot read target position."));
            return;
        }

        Vector3d position = new Vector3d(transform.getPosition());
        position.add(0, config.heightOffset, 0);

        Vector3f rotation = new Vector3f(
            plugin.getConfig().hologram.defaultRotationYaw,
            plugin.getConfig().hologram.defaultRotationPitch,
            plugin.getConfig().hologram.defaultRotationRoll
        );

        String senderName = context.sender().getDisplayName();
        String targetName = targetPlayer == null ? "unknown" : targetPlayer.getUsername();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("sender", senderName);
        placeholders.put("player", targetName);

        String text = MessageUtil.applyPlaceholders(config.text, placeholders);

        plugin.spawnHologram(world, store, position, rotation, text, config);
    }
}
