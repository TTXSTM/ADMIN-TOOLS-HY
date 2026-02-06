package dev.lussuria.admintools.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
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

            String customText = CommandInputUtil.join(args, textStartIndex).trim();
            String titleTemplate = config.title;
            String subtitleTemplate = config.subtitle;
            if (!customText.isBlank()) {
                String[] split = splitTitle(customText);
                titleTemplate = split[0];
                subtitleTemplate = split[1];
            }

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

            EventTitleUtil.showEventTitleToPlayer(
                targetPlayer,
                title,
                subtitle,
                config.force,
                zone,
                config.staySeconds,
                config.fadeInSeconds,
                config.fadeOutSeconds
            );
        });

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
