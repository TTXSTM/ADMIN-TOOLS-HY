package dev.lussuria.admintools;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
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
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import dev.lussuria.admintools.commands.HealCommand;
import dev.lussuria.admintools.commands.OpenUiCommand;
import dev.lussuria.admintools.commands.ShowCommand;
import dev.lussuria.admintools.commands.ShowHologramCommand;
import dev.lussuria.admintools.commands.ShowTitleCommand;
import dev.lussuria.admintools.config.AdminToolsConfig;
import dev.lussuria.admintools.entity.HologramEntity;
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

        registerHologramEntity(cfg.hologram);
        registerCommands(cfg);
        registerEvents(cfg);
    }

    @Override
    protected void shutdown() {
        scheduler.shutdownNow();
        cleanupHolograms();
    }

    public AdminToolsConfig getConfig() {
        return config.get();
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

    private void registerHologramEntity(AdminToolsConfig.Hologram hologram) {
        try {
            EntityModule.get().registerEntity(
                hologram.entityId,
                HologramEntity.class,
                HologramEntity::new,
                null
            );
        } catch (Exception e) {
            getLogger().at(java.util.logging.Level.WARNING).log("Failed to register hologram entity: " + e.getMessage());
        }
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
    }

    private void registerEvents(AdminToolsConfig cfg) {
        EventRegistry events = getEventRegistry();

        events.registerAsyncGlobal(PlayerChatEvent.class, future ->
            future.thenApply(event -> {
                if (!cfg.chat.enabled) {
                    return event;
                }
                event.setFormatter((playerRef, content) -> {
                    String role = resolveRoleForPlayer(playerRef, cfg.chat);
                    Map<String, String> placeholders = Map.of(
                        "player", playerRef.getUsername(),
                        "message", content,
                        "role", role == null ? "" : role
                    );
                    return MessageUtil.renderMessage(cfg.chat.format, placeholders, cfg.chat.parseMessages);
                });
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
            Map<String, String> placeholders = Map.of(
                "player", playerRef.getUsername()
            );

            Message title = MessageUtil.renderMessage(cfg.joinNotification.title, placeholders, cfg.joinNotification.parseMessages);
            Message body = MessageUtil.renderMessage(cfg.joinNotification.body, placeholders, cfg.joinNotification.parseMessages);
            NotificationStyle style = parseNotificationStyle(cfg.joinNotification.style);
            ItemStack iconStack = buildIconStack(cfg.joinNotification.iconItemId);

            if (cfg.joinNotification.sendToUniverse) {
                Store<EntityStore> store = event.getWorld() == null ? null : event.getWorld().getEntityStore().getStore();
                if (store == null) {
                    return;
                }
                if (iconStack == null) {
                    NotificationUtil.sendNotificationToWorld(title, body, null, null, style, store);
                } else {
                    NotificationUtil.sendNotificationToWorld(title, body, null, iconStack.toPacket(), style, store);
                }
            } else {
                if (iconStack == null) {
                    NotificationUtil.sendNotification(playerRef.getPacketHandler(), title, body, style);
                } else {
                    NotificationUtil.sendNotification(playerRef.getPacketHandler(), title, body, iconStack.toPacket(), style);
                }
            }
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
        Store<EntityStore> worldStore = world.getEntityStore().getStore();
        HologramEntity entity = new HologramEntity(world);
        Holder<EntityStore> holder = entity.toHolder();
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.addComponent(Nameplate.getComponentType(), new Nameplate(text));
        holder.addComponent(DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(text)));

        Ref<EntityStore> ref = worldStore.addEntity(holder, AddReason.SPAWN);
        if (ref == null || !ref.isValid()) {
            getLogger().at(java.util.logging.Level.WARNING).log("Failed to spawn hologram entity.");
            return;
        }

        Store<EntityStore> entityStore = ref.getStore();
        EntityModule entityModule = EntityModule.get();
        if (entityModule != null) {
            entityStore.ensureAndGetComponent(ref, entityModule.getVisibleComponentType());
        }

        entityStore.addComponent(ref, Intangible.getComponentType(), Intangible.INSTANCE);
        entityStore.addComponent(ref, Invulnerable.getComponentType(), Invulnerable.INSTANCE);
        if (config.scale > 0f) {
            entityStore.addComponent(ref, EntityScaleComponent.getComponentType(), new EntityScaleComponent(config.scale));
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
