package dev.lussuria.admintools.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import dev.lussuria.admintools.AdminToolsPlugin;
import dev.lussuria.admintools.config.AdminToolsConfig;

import java.util.concurrent.CompletableFuture;

public final class ShowCommand extends AbstractCommand {
    public ShowCommand(AdminToolsPlugin plugin, AdminToolsConfig.ShowRoot root, AdminToolsConfig.ShowTitle title, AdminToolsConfig.ShowHologram hologram) {
        super(root.name, root.description);
        if (root.aliases.length > 0) {
            addAliases(root.aliases);
        }
        if (title.enabled) {
            addSubCommand(new ShowTitleCommand(title));
        }
        if (hologram.enabled) {
            addSubCommand(new ShowHologramCommand(plugin, hologram));
        }
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
        context.sendMessage(Message.raw("Use /" + getName() + " <title|hologram>."));
        return CompletableFuture.completedFuture(null);
    }
}
