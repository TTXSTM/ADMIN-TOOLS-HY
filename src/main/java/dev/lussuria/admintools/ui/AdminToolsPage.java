package dev.lussuria.admintools.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
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

        String title = MessageUtil.applyPlaceholders(ui.title, placeholders);
        String subtitle = MessageUtil.applyPlaceholders(ui.subtitle, placeholders);
        String body = MessageUtil.applyPlaceholders(ui.body, placeholders);

        commands.set("#TitleLabel.Text", title);
        commands.set("#SubtitleLabel.Text", subtitle);
        commands.set("#BodyLabel.Text", body);
    }
}
