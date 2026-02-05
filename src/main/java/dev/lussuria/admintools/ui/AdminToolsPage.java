package dev.lussuria.admintools.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.util.MessageUtil;

import java.util.Map;

public final class AdminToolsPage extends BasicCustomUIPage {
    private final AdminToolsConfig.Ui ui;
    private final PlayerRef playerRef;

    public AdminToolsPage(PlayerRef playerRef, AdminToolsConfig.Ui ui) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.ui = ui;
        this.playerRef = playerRef;
    }

    @Override
    public void build(UICommandBuilder commands) {
        commands.append(ui.uiPath);

        Map<String, String> placeholders = Map.of(
            "player", playerRef.getUsername()
        );

        Message title = MessageUtil.renderMessage(ui.title, placeholders, ui.parseMessages);
        Message subtitle = MessageUtil.renderMessage(ui.subtitle, placeholders, ui.parseMessages);
        Message body = MessageUtil.renderMessage(ui.body, placeholders, ui.parseMessages);

        commands.set("#TitleLabel.Text", title);
        commands.set("#SubtitleLabel.Text", subtitle);
        commands.set("#BodyLabel.Text", body);
    }
}
