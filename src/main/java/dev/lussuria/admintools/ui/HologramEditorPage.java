package dev.lussuria.admintools.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import dev.lussuria.admintools.AdminToolsPlugin;
import dev.lussuria.admintools.hologram.HologramData;
import dev.lussuria.admintools.hologram.HologramManager;

import java.util.logging.Level;

public final class HologramEditorPage extends InteractiveCustomUIPage<HologramEditorEventData> {
    private static final double HEIGHT_STEP = 0.5;

    private final AdminToolsPlugin plugin;
    private final String hologramName;
    private final PlayerRef playerRef;

    public HologramEditorPage(PlayerRef playerRef, AdminToolsPlugin plugin, HologramData hologram) {
        super(playerRef, CustomPageLifetime.CanDismiss, HologramEditorEventData.CODEC);
        this.plugin = plugin;
        this.hologramName = hologram.getName();
        this.playerRef = playerRef;
    }

    @Override
    public void build(
        Ref<EntityStore> playerRef,
        UICommandBuilder commands,
        UIEventBuilder events,
        Store<EntityStore> store
    ) {
        HologramData hologram = plugin.getHologramManager().getHologram(hologramName);
        if (hologram == null) {
            return;
        }

        plugin.getLogger().at(Level.INFO).log("[HoloEditor] build() for: %s", hologramName);
        commands.append("Pages/HologramEditorPage.ui");

        commands.set("#HologramName.Text", hologram.getName());
        commands.set("#PositionLabel.Text", String.format("Position: %.1f, %.1f, %.1f",
            hologram.getPosX(), hologram.getPosY(), hologram.getPosZ()));

        buildLinesList(hologram, commands);

        // Simple action buttons
        bindSimpleAction(events, "#MoveHereButton", "moveHere");
        bindSimpleAction(events, "#DeleteButton", "delete");
        bindSimpleAction(events, "#ExitButton", "exit");
        bindSimpleAction(events, "#HeightUpButton", "heightUp");
        bindSimpleAction(events, "#HeightDownButton", "heightDown");

        // Buttons that read text input - same pattern as HydroHologram
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#AddLineButton",
            new EventData()
                .append(HologramEditorEventData.KEY_ACTION, "addLine")
                .append(HologramEditorEventData.KEY_TEXT, "#NewLineInput.Value"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RemoveLineButton",
            new EventData()
                .append(HologramEditorEventData.KEY_ACTION, "removeLine")
                .append(HologramEditorEventData.KEY_TEXT, "#RemoveLineInput.Value"),
            false
        );
    }

    private void buildLinesList(HologramData hologram, UICommandBuilder commands) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hologram.getLines().size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(i + 1).append(". ").append(hologram.getLines().get(i));
        }
        if (hologram.getLines().isEmpty()) {
            sb.append("(no lines)");
        }
        commands.set("#LinesText.Text", sb.toString());
    }

    private void bindSimpleAction(UIEventBuilder events, String buttonId, String action) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            buttonId,
            EventData.of(HologramEditorEventData.KEY_ACTION, action),
            false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> playerRef, Store<EntityStore> store, HologramEditorEventData data) {
        plugin.getLogger().at(Level.INFO).log("[HoloEditor] handleDataEvent: %s", data);
        if (data == null || data.getAction() == null || data.getAction().isBlank()) {
            plugin.getLogger().at(Level.WARNING).log("[HoloEditor] data/action is null");
            return;
        }

        String action = data.getAction();
        plugin.getLogger().at(Level.INFO).log("[HoloEditor] action=%s, text=%s", action, data.getText());

        HologramManager manager = plugin.getHologramManager();
        HologramData hologram = manager.getHologram(hologramName);
        if (hologram == null) {
            close();
            return;
        }

        switch (action) {
            case "addLine" -> {
                String text = data.getText();
                if (text != null && !text.isBlank()) {
                    manager.addLine(hologram, text.trim());
                    manager.respawnHologram(hologram);
                    manager.save();
                }
                refreshUI(hologram);
            }
            case "removeLine" -> {
                String text = data.getText();
                if (text != null && !text.isBlank()) {
                    try {
                        int idx = Integer.parseInt(text.trim()) - 1;
                        if (idx >= 0 && idx < hologram.getLines().size()) {
                            manager.removeLine(hologram, idx);
                            manager.respawnHologram(hologram);
                            manager.save();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                refreshUI(hologram);
            }
            case "heightUp" -> {
                manager.moveHologram(hologram, hologram.getPosX(), hologram.getPosY() + HEIGHT_STEP, hologram.getPosZ());
                manager.save();
                refreshUI(hologram);
            }
            case "heightDown" -> {
                manager.moveHologram(hologram, hologram.getPosX(), hologram.getPosY() - HEIGHT_STEP, hologram.getPosZ());
                manager.save();
                refreshUI(hologram);
            }
            case "moveHere" -> {
                TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
                if (transform != null && transform.getPosition() != null) {
                    Vector3d pos = new Vector3d(transform.getPosition());
                    manager.moveHologram(hologram, pos.getX(), pos.getY(), pos.getZ());
                    manager.save();
                }
                refreshUI(hologram);
            }
            case "delete" -> {
                manager.deleteHologram(hologramName);
                manager.save();
                close();
            }
            case "exit" -> {
                plugin.getLogger().at(Level.INFO).log("[HoloEditor] closing page");
                close();
            }
        }
    }

    /**
     * In-place UI refresh using sendUpdate() - matches HydroHologram's approach.
     * Does NOT close and reopen the page.
     */
    private void refreshUI(HologramData hologram) {
        plugin.getLogger().at(Level.INFO).log("[HoloEditor] refreshUI via sendUpdate");
        UICommandBuilder commands = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        commands.set("#PositionLabel.Text", String.format("Position: %.1f, %.1f, %.1f",
            hologram.getPosX(), hologram.getPosY(), hologram.getPosZ()));
        commands.set("#NewLineInput.Value", "");
        commands.set("#RemoveLineInput.Value", "");

        buildLinesList(hologram, commands);

        sendUpdate(commands, events, false);
    }
}
