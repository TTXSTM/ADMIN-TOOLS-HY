package dev.lussuria.admintools.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.util.MessageUtil;

import java.util.Map;

public final class AdminToolsPage extends InteractiveCustomUIPage<AdminToolsPageEventData> {
    private final AdminToolsConfig.Ui ui;
    private final AdminToolsConfig.Commands commandsConfig;
    private final PlayerRef playerRef;

    public AdminToolsPage(PlayerRef playerRef, AdminToolsConfig.Ui ui, AdminToolsConfig.Commands commandsConfig) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminToolsPageEventData.CODEC);
        this.ui = ui;
        this.commandsConfig = commandsConfig;
        this.playerRef = playerRef;
    }

    @Override
    public void build(
        Ref<EntityStore> playerRef,
        UICommandBuilder commands,
        UIEventBuilder events,
        Store<EntityStore> store
    ) {
        commands.append(ui.uiPath);

        Map<String, String> placeholders = Map.of(
            "player", this.playerRef.getUsername()
        );

        String title = MessageUtil.applyPlaceholders(ui.title, placeholders);
        String subtitle = MessageUtil.applyPlaceholders(ui.subtitle, placeholders);
        String body = MessageUtil.applyPlaceholders(ui.body, placeholders);

        commands.set("#TitleLabel.Text", title);
        commands.set("#SubtitleLabel.Text", subtitle);
        commands.set("#BodyLabel.Text", body);

        String healCommand = commandsConfig.heal.name;
        String showTitleCommand = buildShowCommand(commandsConfig, commandsConfig.showTitle.name);
        String showHologramCommand = buildShowCommand(commandsConfig, commandsConfig.showHologram.name);
        String openUiCommand = commandsConfig.openUi.name;

        commands.set("#HealButton.Text", "/" + healCommand);
        commands.set("#ShowTitleButton.Text", "/" + showTitleCommand);
        commands.set("#ShowHologramButton.Text", "/" + showHologramCommand);
        commands.set("#OpenUiButton.Text", "/" + openUiCommand);

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#HealButton",
            EventData.of(AdminToolsPageEventData.KEY_COMMAND, healCommand),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ShowTitleButton",
            new EventData()
                .append(AdminToolsPageEventData.KEY_COMMAND, showTitleCommand)
                .append(AdminToolsPageEventData.KEY_TEXT, "#CommandTextInput.Value"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ShowHologramButton",
            new EventData()
                .append(AdminToolsPageEventData.KEY_COMMAND, showHologramCommand)
                .append(AdminToolsPageEventData.KEY_TEXT, "#CommandTextInput.Value"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#OpenUiButton",
            EventData.of(AdminToolsPageEventData.KEY_COMMAND, openUiCommand),
            false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> playerRef, Store<EntityStore> store, AdminToolsPageEventData data) {
        if (data == null || data.getCommand() == null || data.getCommand().isBlank()) {
            return;
        }
        String command = data.getCommand().trim();
        String text = data.getCommandText();
        if (text != null) {
            text = text.trim();
        }
        String fullCommand = command;
        if (text != null && !text.isBlank()) {
            fullCommand = command + " " + text;
        }
        CommandManager.get().handleCommand(this.playerRef, fullCommand);
    }

    private String buildShowCommand(AdminToolsConfig.Commands commands, String subCommand) {
        if (commands.showRoot != null && commands.showRoot.enabled) {
            return commands.showRoot.name + " " + subCommand;
        }
        return subCommand;
    }
}
