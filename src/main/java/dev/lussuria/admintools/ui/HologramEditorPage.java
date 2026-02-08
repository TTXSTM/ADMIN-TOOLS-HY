package dev.lussuria.admintools.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
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

public final class HologramEditorPage extends InteractiveCustomUIPage<HologramEditorEventData> {
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

        commands.append("Pages/HologramEditorPage.ui");

        commands.set("#HologramName.Text", hologram.getName());
        commands.set("#XInput.Value", String.format("%.1f", hologram.getPosX()));
        commands.set("#YInput.Value", String.format("%.1f", hologram.getPosY()));
        commands.set("#ZInput.Value", String.format("%.1f", hologram.getPosZ()));

        buildLinesList(hologram, commands, events);
        bindPositionButtons(events);
        bindActionButtons(events);
    }

    private void buildLinesList(HologramData hologram, UICommandBuilder commands, UIEventBuilder events) {
        for (int i = 0; i < hologram.getLines().size(); i++) {
            String line = hologram.getLines().get(i);
            String lineItemId = "#Line_" + i;

            commands.append("#LinesList", "Pages/HologramLineItem.ui");
            commands.set(lineItemId + " #LineNumber.Text", (i + 1) + ".");
            commands.set(lineItemId + " #LineText.Text", line);

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                lineItemId + " #LineDeleteButton",
                new EventData()
                    .append(HologramEditorEventData.KEY_ACTION, "removeLine")
                    .append(HologramEditorEventData.KEY_LINE_INDEX, String.valueOf(i)),
                false
            );
        }
    }

    private void bindPositionButtons(UIEventBuilder events) {
        bindPositionButton(events, "#XUpButton", "posXUp");
        bindPositionButton(events, "#XDownButton", "posXDown");
        bindPositionButton(events, "#YUpButton", "posYUp");
        bindPositionButton(events, "#YDownButton", "posYDown");
        bindPositionButton(events, "#ZUpButton", "posZUp");
        bindPositionButton(events, "#ZDownButton", "posZDown");

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SetPositionButton",
            new EventData()
                .append(HologramEditorEventData.KEY_ACTION, "setPosition")
                .append(HologramEditorEventData.KEY_POS_X, "#XInput.Value")
                .append(HologramEditorEventData.KEY_POS_Y, "#YInput.Value")
                .append(HologramEditorEventData.KEY_POS_Z, "#ZInput.Value"),
            false
        );
    }

    private void bindPositionButton(UIEventBuilder events, String buttonId, String action) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            buttonId,
            EventData.of(HologramEditorEventData.KEY_ACTION, action),
            false
        );
    }

    private void bindActionButtons(UIEventBuilder events) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#AddLineButton",
            new EventData()
                .append(HologramEditorEventData.KEY_ACTION, "addLine")
                .append(HologramEditorEventData.KEY_NEW_LINE_INPUT, "#NewLineInput.Value"),
            false
        );

        bindSimpleAction(events, "#MoveHereButton", "moveHere");
        bindSimpleAction(events, "#DeleteButton", "delete");
        bindSimpleAction(events, "#ExitButton", "exit");
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
        if (data == null || data.getAction() == null || data.getAction().isBlank()) {
            return;
        }

        HologramManager manager = plugin.getHologramManager();
        HologramData hologram = manager.getHologram(hologramName);
        if (hologram == null) {
            closePage(playerRef, store);
            return;
        }

        switch (data.getAction()) {
            case "addLine" -> {
                String text = data.getNewLineInput();
                if (text != null && !text.isBlank()) {
                    manager.addLine(hologram, text.trim());
                    manager.respawnHologram(hologram);
                    manager.save();
                }
                refreshUI(playerRef, store);
            }
            case "removeLine" -> {
                int idx = data.getLineIndexInt();
                if (idx >= 0 && idx < hologram.getLines().size()) {
                    manager.removeLine(hologram, idx);
                    manager.respawnHologram(hologram);
                    manager.save();
                }
                refreshUI(playerRef, store);
            }
            case "moveHere" -> {
                TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
                if (transform != null && transform.getPosition() != null) {
                    Vector3d pos = new Vector3d(transform.getPosition());
                    manager.moveHologram(hologram, pos.getX(), pos.getY(), pos.getZ());
                    manager.save();
                }
                refreshUI(playerRef, store);
            }
            case "delete" -> {
                manager.deleteHologram(hologramName);
                manager.save();
                closePage(playerRef, store);
            }
            case "exit" -> closePage(playerRef, store);
            case "setPosition" -> {
                double x = data.getPosXDouble(hologram.getPosX());
                double y = data.getPosYDouble(hologram.getPosY());
                double z = data.getPosZDouble(hologram.getPosZ());
                manager.moveHologram(hologram, x, y, z);
                manager.save();
                refreshUI(playerRef, store);
            }
            case "posXUp" -> adjustAndRefresh(hologram, manager, 1, 0, 0, playerRef, store);
            case "posXDown" -> adjustAndRefresh(hologram, manager, -1, 0, 0, playerRef, store);
            case "posYUp" -> adjustAndRefresh(hologram, manager, 0, 1, 0, playerRef, store);
            case "posYDown" -> adjustAndRefresh(hologram, manager, 0, -1, 0, playerRef, store);
            case "posZUp" -> adjustAndRefresh(hologram, manager, 0, 0, 1, playerRef, store);
            case "posZDown" -> adjustAndRefresh(hologram, manager, 0, 0, -1, playerRef, store);
        }
    }

    private void adjustAndRefresh(HologramData h, HologramManager m, double dx, double dy, double dz,
                                  Ref<EntityStore> ref, Store<EntityStore> store) {
        m.moveHologram(h, h.getPosX() + dx, h.getPosY() + dy, h.getPosZ() + dz);
        m.save();
        refreshUI(ref, store);
    }

    private void refreshUI(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player != null) {
            HologramData hologram = plugin.getHologramManager().getHologram(hologramName);
            if (hologram != null) {
                player.getPageManager().openCustomPage(
                    playerRef, store,
                    new HologramEditorPage(this.playerRef, plugin, hologram)
                );
            }
        }
    }

    private void closePage(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        close();
    }
}
