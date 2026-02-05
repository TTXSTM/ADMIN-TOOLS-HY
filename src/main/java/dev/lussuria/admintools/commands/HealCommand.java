package dev.lussuria.admintools.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;

public final class HealCommand extends AbstractTargetPlayerCommand {
    private final AdminToolsConfig.Heal config;

    public HealCommand(AdminToolsConfig.Heal config) {
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
        EntityStatMap statMap = store.getComponent(targetRef, EntityStatsModule.get().getEntityStatMapComponentType());
        if (statMap == null) {
            context.sendMessage(Message.raw("No stat map for target."));
            return;
        }

        EntityStatValue statValue = statMap.get(config.statName);
        if (statValue == null) {
            context.sendMessage(Message.raw("Unknown stat: " + config.statName));
            return;
        }

        statMap.maximizeStatValue(statValue.getIndex());

        String senderName = context.sender().getDisplayName();
        String targetName = targetPlayer == null ? "unknown" : targetPlayer.getUsername();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("sender", senderName);
        placeholders.put("player", targetName);

        context.sendMessage(MessageUtil.renderMessage(config.message, placeholders, config.parseMessages));

        boolean samePlayer = senderRef != null && senderRef.equals(targetRef);
        if (config.sendTargetMessage && targetPlayer != null && !samePlayer) {
            Message targetMessage = MessageUtil.renderMessage(config.messageTarget, placeholders, config.parseMessages);
            targetPlayer.sendMessage(targetMessage);
        }
    }
}
