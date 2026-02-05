package dev.lussuria.admintools.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;

public final class ShowTitleCommand extends AbstractTargetPlayerCommand {
    private final AdminToolsConfig.ShowTitle config;

    public ShowTitleCommand(AdminToolsConfig.ShowTitle config) {
        super(config.name, config.description);
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
        if (targetPlayer == null) {
            return;
        }

        String senderName = context.sender().getDisplayName();
        String targetName = targetPlayer.getUsername();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("sender", senderName);
        placeholders.put("player", targetName);

        String zone = config.zone;
        if (zone == null || zone.isBlank() || "default".equalsIgnoreCase(zone)) {
            zone = EventTitleUtil.DEFAULT_ZONE;
        }

        EventTitleUtil.showEventTitleToPlayer(
            targetPlayer,
            MessageUtil.renderMessage(config.title, placeholders, config.parseMessages),
            MessageUtil.renderMessage(config.subtitle, placeholders, config.parseMessages),
            config.force,
            zone,
            config.staySeconds,
            config.fadeInSeconds,
            config.fadeOutSeconds
        );
    }
}
