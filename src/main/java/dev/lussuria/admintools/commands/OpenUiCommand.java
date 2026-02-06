package dev.lussuria.admintools.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.ui.AdminToolsPage;

public final class OpenUiCommand extends AbstractPlayerCommand {
    private final AdminToolsConfig.OpenUi config;
    private final AdminToolsConfig.Ui ui;
    private final AdminToolsConfig.Commands commands;

    public OpenUiCommand(AdminToolsConfig.OpenUi config, AdminToolsConfig.Ui ui, AdminToolsConfig.Commands commands) {
        super(config.name, config.description);
        this.config = config;
        this.ui = ui;
        this.commands = commands;
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
        Store<EntityStore> store,
        Ref<EntityStore> playerRef,
        PlayerRef playerRefComponent,
        World world
    ) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Player not found."));
            return;
        }

        player.getPageManager().openCustomPage(playerRef, store, new AdminToolsPage(playerRefComponent, ui, commands));
    }
}
