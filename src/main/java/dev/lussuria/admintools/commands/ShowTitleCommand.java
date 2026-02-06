package dev.lussuria.admintools.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.util.CommandInputUtil;
import dev.lussuria.admintools.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ShowTitleCommand extends AbstractCommand {
    private static final String SEPARATOR = "|";
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
        setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        Ref<EntityStore> senderRef = context.isPlayer() ? context.senderAsPlayerRef() : null;
        Store<EntityStore> senderStore = senderRef == null ? null : senderRef.getStore();
        PlayerRef senderPlayer = senderStore == null ? null : senderStore.getComponent(senderRef, PlayerRef.getComponentType());
        World senderWorld = senderStore == null ? null : ((EntityStore) senderStore.getExternalData()).getWorld();

        String[] args = CommandInputUtil.extractArgs(context, this);
        PlayerRef targetPlayer = null;
        int textStartIndex = 0;

        if (senderWorld != null && args.length > 0) {
            targetPlayer = CommandInputUtil.findPlayerByName(senderWorld, args[0]);
            if (targetPlayer != null) {
                textStartIndex = 1;
            }
        }
        if (targetPlayer == null) {
            targetPlayer = senderPlayer;
        }
        if (targetPlayer == null) {
            context.sendMessage(Message.raw("Player not found."));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> targetRef = targetPlayer.getReference();
        if (targetRef == null || !targetRef.isValid()) {
            context.sendMessage(Message.raw("Player not found."));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> targetStore = targetRef.getStore();
        World world = targetStore == null ? null : ((EntityStore) targetStore.getExternalData()).getWorld();
        if (world == null) {
            context.sendMessage(Message.raw("Player is not in a world."));
            return CompletableFuture.completedFuture(null);
        }

        String customText = CommandInputUtil.join(args, textStartIndex).trim();
        String titleTemplate = config.title;
        String subtitleTemplate = config.subtitle;
        if (!customText.isBlank()) {
            String[] split = splitTitle(customText);
            titleTemplate = split[0];
            subtitleTemplate = split[1];
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

        Message title = MessageUtil.renderMessage(titleTemplate, placeholders, config.parseMessages);
        Message subtitle = MessageUtil.renderMessage(subtitleTemplate, placeholders, config.parseMessages);

        PlayerRef target = targetPlayer;
        String finalZone = zone;
        Message finalTitle = title;
        Message finalSubtitle = subtitle;

        world.execute(() -> EventTitleUtil.showEventTitleToPlayer(
            target,
            finalTitle,
            finalSubtitle,
            config.force,
            finalZone,
            config.staySeconds,
            config.fadeInSeconds,
            config.fadeOutSeconds
        ));

        return CompletableFuture.completedFuture(null);
    }

    private String[] splitTitle(String customText) {
        int separatorIndex = customText.indexOf(SEPARATOR);
        if (separatorIndex < 0) {
            return new String[] { customText, "" };
        }
        String title = customText.substring(0, separatorIndex).trim();
        String subtitle = customText.substring(separatorIndex + SEPARATOR.length()).trim();
        return new String[] { title, subtitle };
    }
}
