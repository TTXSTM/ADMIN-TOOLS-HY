package dev.lussuria.admintools;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import dev.lussuria.admintools.commands.HealCommand;
import dev.lussuria.admintools.commands.HologramCommand;
import dev.lussuria.admintools.commands.OpenUiCommand;
import dev.lussuria.admintools.commands.ShowCommand;
import dev.lussuria.admintools.commands.ShowHologramCommand;
import dev.lussuria.admintools.commands.ShowTitleCommand;
import dev.lussuria.admintools.commands.RoleCommand;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.hologram.HologramManager;
import dev.lussuria.admintools.hologram.HologramSpawner;
import dev.lussuria.admintools.role.ChatRole;
import dev.lussuria.admintools.role.RoleManager;
import dev.lussuria.admintools.util.MessageUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AdminToolsPlugin extends JavaPlugin {
    private final Config<AdminToolsConfig> config;
    private final ScheduledExecutorService scheduler;
    private final Set<Ref<EntityStore>> hologramRefs = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> itemCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> joinNotifiedPlayers = ConcurrentHashMap.newKeySet();
    private final EnumSet<InteractionType> allowedInteractionTypes = EnumSet.noneOf(InteractionType.class);
    private HologramManager hologramManager;
    private RoleManager roleManager;

    public AdminToolsPlugin(JavaPluginInit init) {
        super(init);
        this.config = withConfig(AdminToolsConfig.CODEC);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AdminTools-Scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    protected void setup() {
        AdminToolsConfig cfg = config.get();
        ensureConfigSaved();
        configureInteractionTypes(cfg.customItem);

        hologramManager = new HologramManager(getLogger(), getDataDirectory(), cfg.commands.hologramCommands.defaultScale);
        hologramManager.load();

        roleManager = new RoleManager(getLogger(), getDataDirectory());
        roleManager.load();

        registerCommands(cfg);
        registerEvents(cfg);

        scheduler.schedule(() -> hologramManager.spawnAllHolograms(), 2, TimeUnit.SECONDS);
    }

    @Override
    protected void shutdown() {
        scheduler.shutdownNow();
        if (hologramManager != null) {
            hologramManager.despawnAllHolograms();
            hologramManager.save();
        }
        if (roleManager != null) {
            roleManager.save();
        }
        cleanupHolograms();
    }

    public AdminToolsConfig getConfig() {
        return config.get();
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    private void ensureConfigSaved() {
        try {
            Path dataDir = getDataDirectory();
            Files.createDirectories(dataDir);
            Path configPath = dataDir.resolve("config.json");
            if (!Files.exists(configPath)) {
                config.save().join();
            }
        } catch (Exception e) {
            getLogger().at(java.util.logging.Level.WARNING).log("Failed to write default config: " + e.getMessage());
        }
    }

    private void configureInteractionTypes(AdminToolsConfig.CustomItem customItem) {
        allowedInteractionTypes.clear();
        if (customItem.interactionTypes == null || customItem.interactionTypes.length == 0) {
            return;
        }
        for (String typeName : customItem.interactionTypes) {
            if (typeName == null || typeName.isBlank()) {
                continue;
            }
            try {
                allowedInteractionTypes.add(InteractionType.valueOf(typeName));
            } catch (IllegalArgumentException ignored) {
                getLogger().at(java.util.logging.Level.WARNING).log("Unknown interaction type: " + typeName);
            }
        }
    }

    private String resolveRoleForPlayer(PlayerRef playerRef, AdminToolsConfig.Chat chat) {
        if (chat == null || !chat.includeRole) {
            return "";
        }
        if (playerRef == null || playerRef.getUuid() == null) {
            return chat.defaultRole;
        }
        Set<String> groups = null;
        try {
            PermissionsModule permissions = PermissionsModule.get();
            if (permissions != null) {
                groups = permissions.getGroupsForUser(playerRef.getUuid());
            }
        } catch (Exception ignored) {
            // Best-effort.
        }
        return resolveRoleFromGroups(groups, chat);
    }

    private String resolveRoleFromGroups(Set<String> groups, AdminToolsConfig.Chat chat) {
        String selectedGroup = selectGroup(groups, chat.rolePriority);
        Map<String, String> mappings = parseRoleMappings(chat.roleMappings);
        if (selectedGroup != null && mappings.containsKey(selectedGroup)) {
            return mappings.get(selectedGroup);
        }
        if (selectedGroup != null && chat.useGroupNameIfNoMapping) {
            return selectedGroup;
        }
        return chat.defaultRole;
    }

    private String selectGroup(Set<String> groups, String[] priority) {
        if (groups == null || groups.isEmpty()) {
            return null;
        }
        Set<String> upperGroups = new HashSet<>();
        for (String group : groups) {
            if (group == null || group.isBlank()) {
                continue;
            }
            upperGroups.add(group.toUpperCase(Locale.ROOT));
        }
        if (priority != null) {
            for (String group : priority) {
                if (group == null || group.isBlank()) {
                    continue;
                }
                String key = group.toUpperCase(Locale.ROOT);
                if (upperGroups.contains(key)) {
                    return key;
                }
            }
        }
        ArrayList<String> sorted = new ArrayList<>(upperGroups);
        Collections.sort(sorted);
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    private Map<String, String> parseRoleMappings(String[] mappings) {
        Map<String, String> result = new LinkedHashMap<>();
        if (mappings == null) {
            return result;
        }
        for (String entry : mappings) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int idx = entry.indexOf('=');
            if (idx < 0) {
                idx = entry.indexOf(':');
            }
            if (idx < 0) {
                continue;
            }
            String key = entry.substring(0, idx).trim();
            String value = entry.substring(idx + 1).trim();
            if (!key.isEmpty()) {
                result.put(key.toUpperCase(Locale.ROOT), value);
            }
        }
        return result;
    }

    private Message buildChatMessage(PlayerRef playerRef, String content, AdminToolsConfig.Chat chat) {
        // Try dynamic roles first
        ChatRole resolved = null;
        if (chat.includeRole && roleManager != null && playerRef != null && playerRef.getUuid() != null) {
            Set<String> groups = null;
            try {
                PermissionsModule permissions = PermissionsModule.get();
                if (permissions != null) {
                    groups = permissions.getGroupsForUser(playerRef.getUuid());
                }
            } catch (Exception ignored) {
            }
            resolved = roleManager.resolveRole(playerRef.getUuid(), groups);
        }

        // If a ChatRole was found, build a rich colored message
        if (resolved != null) {
            Message result = Message.empty();

            Message roleMsg = Message.raw(resolved.getDisplayName() + " ").color(resolved.getColor());
            if (resolved.isBold()) {
                roleMsg = roleMsg.bold(true);
            }
            if (resolved.isItalic()) {
                roleMsg = roleMsg.italic(true);
            }
            result.insert(roleMsg);

            result.insert(Message.raw(playerRef.getUsername()).color(chat.nameColor));
            result.insert(Message.raw(" >> ").color(chat.separatorColor));

            Message contentMsg;
            if (chat.parseMessages) {
                try {
                    contentMsg = Message.parse(content);
                } catch (Exception e) {
                    contentMsg = Message.raw(content);
                }
            } else {
                contentMsg = Message.raw(content);
            }
            contentMsg = contentMsg.color(chat.messageColor);
            result.insert(contentMsg);

            return result;
        }

        // Fallback to legacy format string
        String role = resolveRoleForPlayer(playerRef, chat);
        Map<String, String> placeholders = Map.of(
            "player", playerRef.getUsername(),
            "message", content,
            "role", role == null ? "" : role
        );
        return MessageUtil.renderMessage(chat.format, placeholders, chat.parseMessages);
    }

    private void registerCommands(AdminToolsConfig cfg) {
        CommandRegistry commands = getCommandRegistry();

        if (cfg.commands.heal.enabled) {
            commands.registerCommand(new HealCommand(cfg.commands.heal));
        }

        if (cfg.commands.showRoot.enabled) {
            commands.registerCommand(new ShowCommand(this, cfg.commands.showRoot, cfg.commands.showTitle, cfg.commands.showHologram));
        } else {
            if (cfg.commands.showTitle.enabled) {
                commands.registerCommand(new ShowTitleCommand(cfg.commands.showTitle));
            }
            if (cfg.commands.showHologram.enabled) {
                commands.registerCommand(new ShowHologramCommand(this, cfg.commands.showHologram));
            }
        }

        if (cfg.commands.openUi.enabled) {
            commands.registerCommand(new OpenUiCommand(cfg.commands.openUi, cfg.ui, cfg.commands));
        }

        if (cfg.commands.hologramCommands.enabled) {
            commands.registerCommand(new HologramCommand(this, cfg.commands.hologramCommands));
        }

        if (cfg.commands.roleCommands.enabled) {
            commands.registerCommand(new RoleCommand(this, cfg.commands.roleCommands));
        }
    }

    private void registerEvents(AdminToolsConfig cfg) {
        EventRegistry events = getEventRegistry();

        events.registerAsyncGlobal(PlayerChatEvent.class, future ->
            future.thenApply(event -> {
                if (!cfg.chat.enabled) {
                    return event;
                }
                event.setFormatter((playerRef, content) -> buildChatMessage(playerRef, content, cfg.chat));
                return event;
            })
        );

        events.registerGlobal(AddPlayerToWorldEvent.class, event -> {
            if (!cfg.joinNotification.enabled) {
                return;
            }

            PlayerRef playerRef = null;
            if (event.getHolder() != null) {
                playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
            }
            if (playerRef == null) {
                return;
            }
            UUID playerUuid = playerRef.getUuid();
            if (playerUuid != null && !joinNotifiedPlayers.add(playerUuid)) {
                return;
            }

            final String username = playerRef.getUsername();
            final PlayerRef capturedRef = playerRef;
            final World world = event.getWorld();

            // Delay notification so the player's client is fully loaded
            scheduler.schedule(() -> {
                try {
                    Map<String, String> placeholders = Map.of("player", username);
                    Message title = MessageUtil.renderMessage(cfg.joinNotification.title, placeholders, cfg.joinNotification.parseMessages);
                    Message body = MessageUtil.renderMessage(cfg.joinNotification.body, placeholders, cfg.joinNotification.parseMessages);
                    NotificationStyle style = parseNotificationStyle(cfg.joinNotification.style);
                    ItemStack iconStack = buildIconStack(cfg.joinNotification.iconItemId);

                    if (cfg.joinNotification.sendToUniverse && world != null) {
                        world.execute(() -> {
                            Store<EntityStore> store = world.getEntityStore().getStore();
                            if (store == null) {
                                return;
                            }
                            if (iconStack == null) {
                                NotificationUtil.sendNotificationToWorld(title, body, null, null, style, store);
                            } else {
                                NotificationUtil.sendNotificationToWorld(title, body, null, iconStack.toPacket(), style, store);
                            }
                        });
                    } else {
                        if (iconStack == null) {
                            NotificationUtil.sendNotification(capturedRef.getPacketHandler(), title, body, style);
                        } else {
                            NotificationUtil.sendNotification(capturedRef.getPacketHandler(), title, body, iconStack.toPacket(), style);
                        }
                    }
                } catch (Exception e) {
                    getLogger().at(java.util.logging.Level.WARNING).log("Failed to send join notification: " + e.getMessage());
                }
            }, 2, TimeUnit.SECONDS);
        });

        events.register(PlayerDisconnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            if (playerRef != null) {
                joinNotifiedPlayers.remove(playerRef.getUuid());
            }
        });

        events.registerGlobal(PlayerInteractEvent.class, event -> {
            if (!cfg.customItem.enabled) {
                return;
            }
            if (!allowedInteractionTypes.isEmpty() && !allowedInteractionTypes.contains(event.getActionType())) {
                return;
            }

            ItemStack item = event.getItemInHand();
            if (item == null || item.isEmpty() || !item.isValid()) {
                return;
            }
            if (!isCustomItem(item, cfg.customItem)) {
                return;
            }

            Ref<EntityStore> playerRef = event.getPlayerRef();
            Store<EntityStore> store = playerRef.getStore();
            PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent == null) {
                return;
            }

            if (!cooldownReady(playerRefComponent.getUuid(), cfg.customItem.cooldownSeconds)) {
                if (cfg.customItem.cooldownMessage != null && !cfg.customItem.cooldownMessage.isBlank()) {
                    Map<String, String> placeholders = Map.of("player", playerRefComponent.getUsername());
                    playerRefComponent.sendMessage(
                        MessageUtil.renderMessage(cfg.customItem.cooldownMessage, placeholders, cfg.customItem.parseMessages)
                    );
                }
                return;
            }

            if (cfg.customItem.cancelInteraction) {
                event.setCancelled(true);
            }

            TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform == null || transform.getPosition() == null) {
                return;
            }

            Vector3d position = new Vector3d(transform.getPosition());
            spawnLightning(cfg.customItem, position, store);
            playLightningSound(cfg.customItem, position, store);
        });
    }

    private boolean isCustomItem(ItemStack stack, AdminToolsConfig.CustomItem customItem) {
        if (customItem.matchItemId && customItem.itemId != null && !customItem.itemId.isBlank()) {
            if (customItem.itemId.equalsIgnoreCase(stack.getItemId())) {
                return true;
            }
        }
        if (customItem.matchMetadata && customItem.metadataKey != null && !customItem.metadataKey.isBlank()) {
            return stack.getMetadata() != null && stack.getMetadata().containsKey(customItem.metadataKey);
        }
        return false;
    }

    private boolean cooldownReady(UUID uuid, float cooldownSeconds) {
        if (cooldownSeconds <= 0f) {
            return true;
        }
        long now = System.currentTimeMillis();
        long cooldownMillis = (long) (cooldownSeconds * 1000.0f);
        Long lastUse = itemCooldowns.put(uuid, now);
        return lastUse == null || now - lastUse >= cooldownMillis;
    }

    private void spawnLightning(AdminToolsConfig.CustomItem customItem, Vector3d base, Store<EntityStore> store) {
        if (customItem.particleSystemId == null || customItem.particleSystemId.isBlank()) {
            return;
        }
        int count = Math.max(1, customItem.lightningCount);
        float spread = Math.max(0f, customItem.lightningSpread);
        for (int i = 0; i < count; i++) {
            Vector3d position = new Vector3d(base);
            if (spread > 0f) {
                double dx = (Math.random() - 0.5) * spread;
                double dz = (Math.random() - 0.5) * spread;
                position.add(dx, 0, dz);
            }
            ParticleUtil.spawnParticleEffect(customItem.particleSystemId, position, store);
        }
    }

    private void playLightningSound(AdminToolsConfig.CustomItem customItem, Vector3d position, Store<EntityStore> store) {
        if (customItem.soundEventId == null || customItem.soundEventId.isBlank()) {
            return;
        }
        int soundIndex = SoundEvent.getAssetMap().getIndex(customItem.soundEventId);
        if (soundIndex == Integer.MIN_VALUE) {
            getLogger().at(java.util.logging.Level.WARNING).log("Unknown sound event: " + customItem.soundEventId);
            return;
        }
        SoundCategory category = parseSoundCategory(customItem.soundCategory);
        SoundUtil.playSoundEvent3d(
            soundIndex,
            category,
            position.getX(),
            position.getY(),
            position.getZ(),
            customItem.soundVolume,
            customItem.soundPitch,
            store
        );
    }

    private SoundCategory parseSoundCategory(String value) {
        if (value == null || value.isBlank()) {
            return SoundCategory.SFX;
        }
        try {
            return SoundCategory.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return SoundCategory.SFX;
        }
    }

    private NotificationStyle parseNotificationStyle(String value) {
        if (value == null || value.isBlank()) {
            return NotificationStyle.Default;
        }
        try {
            return NotificationStyle.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return NotificationStyle.Default;
        }
    }

    private ItemStack buildIconStack(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        ItemStack stack = new ItemStack(itemId);
        return stack.isValid() ? stack : null;
    }

    public void spawnHologram(
        World world,
        Store<EntityStore> store,
        Vector3d position,
        Vector3f rotation,
        String text,
        AdminToolsConfig.ShowHologram config
    ) {
        Ref<EntityStore> ref = HologramSpawner.spawnLine(world, position, text, config.scale);
        if (ref == null || !ref.isValid()) {
            getLogger().at(java.util.logging.Level.WARNING).log("Failed to spawn hologram entity.");
            return;
        }

        hologramRefs.add(ref);
        scheduleHologramRemoval(ref, config.durationSeconds);
    }

    private void scheduleHologramRemoval(Ref<EntityStore> ref, float durationSeconds) {
        if (durationSeconds <= 0f) {
            return;
        }
        long delayMs = (long) (durationSeconds * 1000.0f);
        scheduler.schedule(() -> {
            try {
                Store<EntityStore> store = ref.getStore();
                EntityStore entityStore = (EntityStore) store.getExternalData();
                World world = entityStore.getWorld();
                world.execute(() -> {
                    if (ref.isValid()) {
                        store.removeEntity(ref, RemoveReason.REMOVE);
                    }
                });
            } catch (Exception ignored) {
                // Best-effort cleanup.
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void cleanupHolograms() {
        for (Ref<EntityStore> ref : hologramRefs) {
            try {
                Store<EntityStore> store = ref.getStore();
                EntityStore entityStore = (EntityStore) store.getExternalData();
                World world = entityStore.getWorld();
                world.execute(() -> {
                    if (ref.isValid()) {
                        store.removeEntity(ref, RemoveReason.REMOVE);
                    }
                });
            } catch (Exception ignored) {
                // Best-effort cleanup.
            }
        }
        hologramRefs.clear();
    }
}
