package dev.lussuria.admintools.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class AdminToolsConfig {
    public static final BuilderCodec<AdminToolsConfig> CODEC = BuilderCodec.builder(AdminToolsConfig.class, AdminToolsConfig::new)
        .addField(new KeyedCodec<>("Commands", Commands.CODEC), (c, v) -> c.commands = v, c -> c.commands)
        .addField(new KeyedCodec<>("Chat", Chat.CODEC), (c, v) -> c.chat = v, c -> c.chat)
        .addField(new KeyedCodec<>("JoinNotification", JoinNotification.CODEC), (c, v) -> c.joinNotification = v, c -> c.joinNotification)
        .addField(new KeyedCodec<>("CustomItem", CustomItem.CODEC), (c, v) -> c.customItem = v, c -> c.customItem)
        .addField(new KeyedCodec<>("Hologram", Hologram.CODEC), (c, v) -> c.hologram = v, c -> c.hologram)
        .addField(new KeyedCodec<>("Ui", Ui.CODEC), (c, v) -> c.ui = v, c -> c.ui)
        .build();

    public Commands commands = new Commands();
    public Chat chat = new Chat();
    public JoinNotification joinNotification = new JoinNotification();
    public CustomItem customItem = new CustomItem();
    public Hologram hologram = new Hologram();
    public Ui ui = new Ui();

    public static final class Commands {
        public static final BuilderCodec<Commands> CODEC = BuilderCodec.builder(Commands.class, Commands::new)
            .addField(new KeyedCodec<>("ShowRoot", ShowRoot.CODEC), (c, v) -> c.showRoot = v, c -> c.showRoot)
            .addField(new KeyedCodec<>("Heal", Heal.CODEC), (c, v) -> c.heal = v, c -> c.heal)
            .addField(new KeyedCodec<>("ShowTitle", ShowTitle.CODEC), (c, v) -> c.showTitle = v, c -> c.showTitle)
            .addField(new KeyedCodec<>("ShowHologram", ShowHologram.CODEC), (c, v) -> c.showHologram = v, c -> c.showHologram)
            .addField(new KeyedCodec<>("OpenUi", OpenUi.CODEC), (c, v) -> c.openUi = v, c -> c.openUi)
            .addField(new KeyedCodec<>("HologramCommands", HologramCommands.CODEC), (c, v) -> c.hologramCommands = v, c -> c.hologramCommands)
            .addField(new KeyedCodec<>("RoleCommands", RoleCommands.CODEC), (c, v) -> c.roleCommands = v, c -> c.roleCommands)
            .build();

        public ShowRoot showRoot = new ShowRoot();
        public Heal heal = new Heal();
        public ShowTitle showTitle = new ShowTitle();
        public ShowHologram showHologram = new ShowHologram();
        public OpenUi openUi = new OpenUi();
        public HologramCommands hologramCommands = new HologramCommands();
        public RoleCommands roleCommands = new RoleCommands();
    }

    public static final class ShowRoot {
        public static final BuilderCodec<ShowRoot> CODEC = BuilderCodec.builder(ShowRoot.class, ShowRoot::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("Name", Codec.STRING), (c, v) -> c.name = v, c -> c.name)
            .addField(new KeyedCodec<>("Aliases", Codec.STRING_ARRAY), (c, v) -> c.aliases = v, c -> c.aliases)
            .addField(new KeyedCodec<>("Description", Codec.STRING), (c, v) -> c.description = v, c -> c.description)
            .build();

        public boolean enabled = true;
        public String name = "show";
        public String[] aliases = new String[0];
        public String description = "Show admin visuals.";
    }

    public static final class Heal {
        public static final BuilderCodec<Heal> CODEC = BuilderCodec.builder(Heal.class, Heal::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("Name", Codec.STRING), (c, v) -> c.name = v, c -> c.name)
            .addField(new KeyedCodec<>("Aliases", Codec.STRING_ARRAY), (c, v) -> c.aliases = v, c -> c.aliases)
            .addField(new KeyedCodec<>("Description", Codec.STRING), (c, v) -> c.description = v, c -> c.description)
            .addField(new KeyedCodec<>("Permission", Codec.STRING), (c, v) -> c.permission = v, c -> c.permission)
            .addField(new KeyedCodec<>("StatName", Codec.STRING), (c, v) -> c.statName = v, c -> c.statName)
            .addField(new KeyedCodec<>("Message", Codec.STRING), (c, v) -> c.message = v, c -> c.message)
            .addField(new KeyedCodec<>("MessageTarget", Codec.STRING), (c, v) -> c.messageTarget = v, c -> c.messageTarget)
            .addField(new KeyedCodec<>("SendTargetMessage", Codec.BOOLEAN), (c, v) -> c.sendTargetMessage = v, c -> c.sendTargetMessage)
            .addField(new KeyedCodec<>("ParseMessages", Codec.BOOLEAN), (c, v) -> c.parseMessages = v, c -> c.parseMessages)
            .build();

        public boolean enabled = true;
        public String name = "heal";
        public String[] aliases = new String[0];
        public String description = "Heal a player.";
        public String permission = "admintools.command.heal";
        public String statName = "Health";
        public String message = "Healed {player}.";
        public String messageTarget = "You were healed by {sender}.";
        public boolean sendTargetMessage = true;
        public boolean parseMessages = true;
    }

    public static final class ShowTitle {
        public static final BuilderCodec<ShowTitle> CODEC = BuilderCodec.builder(ShowTitle.class, ShowTitle::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("Name", Codec.STRING), (c, v) -> c.name = v, c -> c.name)
            .addField(new KeyedCodec<>("Aliases", Codec.STRING_ARRAY), (c, v) -> c.aliases = v, c -> c.aliases)
            .addField(new KeyedCodec<>("Description", Codec.STRING), (c, v) -> c.description = v, c -> c.description)
            .addField(new KeyedCodec<>("Permission", Codec.STRING), (c, v) -> c.permission = v, c -> c.permission)
            .addField(new KeyedCodec<>("Title", Codec.STRING), (c, v) -> c.title = v, c -> c.title)
            .addField(new KeyedCodec<>("Subtitle", Codec.STRING), (c, v) -> c.subtitle = v, c -> c.subtitle)
            .addField(new KeyedCodec<>("Force", Codec.BOOLEAN), (c, v) -> c.force = v, c -> c.force)
            .addField(new KeyedCodec<>("Zone", Codec.STRING), (c, v) -> c.zone = v, c -> c.zone)
            .addField(new KeyedCodec<>("FadeInSeconds", Codec.FLOAT), (c, v) -> c.fadeInSeconds = v, c -> c.fadeInSeconds)
            .addField(new KeyedCodec<>("StaySeconds", Codec.FLOAT), (c, v) -> c.staySeconds = v, c -> c.staySeconds)
            .addField(new KeyedCodec<>("FadeOutSeconds", Codec.FLOAT), (c, v) -> c.fadeOutSeconds = v, c -> c.fadeOutSeconds)
            .addField(new KeyedCodec<>("ParseMessages", Codec.BOOLEAN), (c, v) -> c.parseMessages = v, c -> c.parseMessages)
            .build();

        public boolean enabled = true;
        public String name = "title";
        public String[] aliases = new String[0];
        public String description = "Show a title to a player.";
        public String permission = "admintools.command.show.title";
        public String title = "Admin Tools";
        public String subtitle = "Hello, {player}!";
        public boolean force = true;
        public String zone = "default";
        public float fadeInSeconds = 0.5f;
        public float staySeconds = 2.0f;
        public float fadeOutSeconds = 0.5f;
        public boolean parseMessages = true;
    }

    public static final class ShowHologram {
        public static final BuilderCodec<ShowHologram> CODEC = BuilderCodec.builder(ShowHologram.class, ShowHologram::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("Name", Codec.STRING), (c, v) -> c.name = v, c -> c.name)
            .addField(new KeyedCodec<>("Aliases", Codec.STRING_ARRAY), (c, v) -> c.aliases = v, c -> c.aliases)
            .addField(new KeyedCodec<>("Description", Codec.STRING), (c, v) -> c.description = v, c -> c.description)
            .addField(new KeyedCodec<>("Permission", Codec.STRING), (c, v) -> c.permission = v, c -> c.permission)
            .addField(new KeyedCodec<>("Text", Codec.STRING), (c, v) -> c.text = v, c -> c.text)
            .addField(new KeyedCodec<>("HeightOffset", Codec.FLOAT), (c, v) -> c.heightOffset = v, c -> c.heightOffset)
            .addField(new KeyedCodec<>("Scale", Codec.FLOAT), (c, v) -> c.scale = v, c -> c.scale)
            .addField(new KeyedCodec<>("DurationSeconds", Codec.FLOAT), (c, v) -> c.durationSeconds = v, c -> c.durationSeconds)
            .addField(new KeyedCodec<>("ParseMessages", Codec.BOOLEAN), (c, v) -> c.parseMessages = v, c -> c.parseMessages)
            .build();

        public boolean enabled = true;
        public String name = "hologram";
        public String[] aliases = new String[0];
        public String description = "Show a hologram at a player.";
        public String permission = "admintools.command.show.hologram";
        public String text = "AdminTools Hologram";
        public float heightOffset = 2.0f;
        public float scale = 1.0f;
        public float durationSeconds = 10.0f;
        public boolean parseMessages = true;
    }

    public static final class OpenUi {
        public static final BuilderCodec<OpenUi> CODEC = BuilderCodec.builder(OpenUi.class, OpenUi::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("Name", Codec.STRING), (c, v) -> c.name = v, c -> c.name)
            .addField(new KeyedCodec<>("Aliases", Codec.STRING_ARRAY), (c, v) -> c.aliases = v, c -> c.aliases)
            .addField(new KeyedCodec<>("Description", Codec.STRING), (c, v) -> c.description = v, c -> c.description)
            .addField(new KeyedCodec<>("Permission", Codec.STRING), (c, v) -> c.permission = v, c -> c.permission)
            .build();

        public boolean enabled = true;
        public String name = "openui";
        public String[] aliases = new String[0];
        public String description = "Open the AdminTools UI.";
        public String permission = "admintools.command.openui";
    }

    public static final class RoleCommands {
        public static final BuilderCodec<RoleCommands> CODEC = BuilderCodec.builder(RoleCommands.class, RoleCommands::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("Name", Codec.STRING), (c, v) -> c.name = v, c -> c.name)
            .addField(new KeyedCodec<>("Aliases", Codec.STRING_ARRAY), (c, v) -> c.aliases = v, c -> c.aliases)
            .addField(new KeyedCodec<>("Description", Codec.STRING), (c, v) -> c.description = v, c -> c.description)
            .addField(new KeyedCodec<>("Permission", Codec.STRING), (c, v) -> c.permission = v, c -> c.permission)
            .build();

        public boolean enabled = true;
        public String name = "role";
        public String[] aliases = new String[] { "chatrole" };
        public String description = "Manage chat roles.";
        public String permission = "admintools.command.role";
    }

    public static final class HologramCommands {
        public static final BuilderCodec<HologramCommands> CODEC = BuilderCodec.builder(HologramCommands.class, HologramCommands::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("Name", Codec.STRING), (c, v) -> c.name = v, c -> c.name)
            .addField(new KeyedCodec<>("Aliases", Codec.STRING_ARRAY), (c, v) -> c.aliases = v, c -> c.aliases)
            .addField(new KeyedCodec<>("Description", Codec.STRING), (c, v) -> c.description = v, c -> c.description)
            .addField(new KeyedCodec<>("Permission", Codec.STRING), (c, v) -> c.permission = v, c -> c.permission)
            .addField(new KeyedCodec<>("DefaultLineSpacing", Codec.FLOAT), (c, v) -> c.defaultLineSpacing = v, c -> c.defaultLineSpacing)
            .addField(new KeyedCodec<>("DefaultScale", Codec.FLOAT), (c, v) -> c.defaultScale = v, c -> c.defaultScale)
            .build();

        public boolean enabled = true;
        public String name = "holo";
        public String[] aliases = new String[] { "hologram" };
        public String description = "Manage persistent holograms.";
        public String permission = "admintools.command.holo";
        public float defaultLineSpacing = 0.25f;
        public float defaultScale = 1.0f;
    }

    public static final class Chat {
        public static final BuilderCodec<Chat> CODEC = BuilderCodec.builder(Chat.class, Chat::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("Format", Codec.STRING), (c, v) -> c.format = v, c -> c.format)
            .addField(new KeyedCodec<>("ParseMessages", Codec.BOOLEAN), (c, v) -> c.parseMessages = v, c -> c.parseMessages)
            .addField(new KeyedCodec<>("IncludeRole", Codec.BOOLEAN), (c, v) -> c.includeRole = v, c -> c.includeRole)
            .addField(new KeyedCodec<>("DefaultRole", Codec.STRING), (c, v) -> c.defaultRole = v, c -> c.defaultRole)
            .addField(new KeyedCodec<>("UseGroupNameIfNoMapping", Codec.BOOLEAN), (c, v) -> c.useGroupNameIfNoMapping = v, c -> c.useGroupNameIfNoMapping)
            .addField(new KeyedCodec<>("RolePriority", Codec.STRING_ARRAY), (c, v) -> c.rolePriority = v, c -> c.rolePriority)
            .addField(new KeyedCodec<>("RoleMappings", Codec.STRING_ARRAY), (c, v) -> c.roleMappings = v, c -> c.roleMappings)
            .addField(new KeyedCodec<>("NameColor", Codec.STRING), (c, v) -> c.nameColor = v, c -> c.nameColor)
            .addField(new KeyedCodec<>("MessageColor", Codec.STRING), (c, v) -> c.messageColor = v, c -> c.messageColor)
            .addField(new KeyedCodec<>("SeparatorColor", Codec.STRING), (c, v) -> c.separatorColor = v, c -> c.separatorColor)
            .build();

        public boolean enabled = true;
        public String format = "{role} {player} >> {message}";
        public boolean parseMessages = true;
        public boolean includeRole = true;
        public String defaultRole = "Player";
        public boolean useGroupNameIfNoMapping = true;
        public String[] rolePriority = new String[] { "OP", "Admin", "Moderator", "Mod", "Default" };
        public String[] roleMappings = new String[] {
            "OP=[Admin]",
            "Admin=[Admin]",
            "Moderator=[Mod]",
            "Mod=[Mod]",
            "Default=[Player]"
        };
        public String nameColor = "#ffffff";
        public String messageColor = "#aaaaaa";
        public String separatorColor = "#555555";
    }

    public static final class JoinNotification {
        public static final BuilderCodec<JoinNotification> CODEC = BuilderCodec.builder(JoinNotification.class, JoinNotification::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("SendToUniverse", Codec.BOOLEAN), (c, v) -> c.sendToUniverse = v, c -> c.sendToUniverse)
            .addField(new KeyedCodec<>("Title", Codec.STRING), (c, v) -> c.title = v, c -> c.title)
            .addField(new KeyedCodec<>("Body", Codec.STRING), (c, v) -> c.body = v, c -> c.body)
            .addField(new KeyedCodec<>("Style", Codec.STRING), (c, v) -> c.style = v, c -> c.style)
            .addField(new KeyedCodec<>("IconItemId", Codec.STRING), (c, v) -> c.iconItemId = v, c -> c.iconItemId)
            .addField(new KeyedCodec<>("ParseMessages", Codec.BOOLEAN), (c, v) -> c.parseMessages = v, c -> c.parseMessages)
            .build();

        public boolean enabled = true;
        public boolean sendToUniverse = true;
        public String title = "Player Joined";
        public String body = "{player} joined the server.";
        public String style = "Success";
        public String iconItemId = "";
        public boolean parseMessages = true;
    }

    public static final class CustomItem {
        public static final BuilderCodec<CustomItem> CODEC = BuilderCodec.builder(CustomItem.class, CustomItem::new)
            .addField(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v, c -> c.enabled)
            .addField(new KeyedCodec<>("ItemId", Codec.STRING), (c, v) -> c.itemId = v, c -> c.itemId)
            .addField(new KeyedCodec<>("MetadataKey", Codec.STRING), (c, v) -> c.metadataKey = v, c -> c.metadataKey)
            .addField(new KeyedCodec<>("MatchItemId", Codec.BOOLEAN), (c, v) -> c.matchItemId = v, c -> c.matchItemId)
            .addField(new KeyedCodec<>("MatchMetadata", Codec.BOOLEAN), (c, v) -> c.matchMetadata = v, c -> c.matchMetadata)
            .addField(new KeyedCodec<>("InteractionTypes", Codec.STRING_ARRAY), (c, v) -> c.interactionTypes = v, c -> c.interactionTypes)
            .addField(new KeyedCodec<>("SoundEventId", Codec.STRING), (c, v) -> c.soundEventId = v, c -> c.soundEventId)
            .addField(new KeyedCodec<>("SoundCategory", Codec.STRING), (c, v) -> c.soundCategory = v, c -> c.soundCategory)
            .addField(new KeyedCodec<>("SoundVolume", Codec.FLOAT), (c, v) -> c.soundVolume = v, c -> c.soundVolume)
            .addField(new KeyedCodec<>("SoundPitch", Codec.FLOAT), (c, v) -> c.soundPitch = v, c -> c.soundPitch)
            .addField(new KeyedCodec<>("ParticleSystemId", Codec.STRING), (c, v) -> c.particleSystemId = v, c -> c.particleSystemId)
            .addField(new KeyedCodec<>("LightningCount", Codec.INTEGER), (c, v) -> c.lightningCount = v, c -> c.lightningCount)
            .addField(new KeyedCodec<>("LightningSpread", Codec.FLOAT), (c, v) -> c.lightningSpread = v, c -> c.lightningSpread)
            .addField(new KeyedCodec<>("CooldownSeconds", Codec.FLOAT), (c, v) -> c.cooldownSeconds = v, c -> c.cooldownSeconds)
            .addField(new KeyedCodec<>("CancelInteraction", Codec.BOOLEAN), (c, v) -> c.cancelInteraction = v, c -> c.cancelInteraction)
            .addField(new KeyedCodec<>("CooldownMessage", Codec.STRING), (c, v) -> c.cooldownMessage = v, c -> c.cooldownMessage)
            .addField(new KeyedCodec<>("ParseMessages", Codec.BOOLEAN), (c, v) -> c.parseMessages = v, c -> c.parseMessages)
            .build();

        public boolean enabled = true;
        public String itemId = "AdminTools_Lightning_Wand";
        public String metadataKey = "AdminTools.CustomItem";
        public boolean matchItemId = true;
        public boolean matchMetadata = false;
        public String[] interactionTypes = new String[] { "Primary" };
        public String soundEventId = "SFX_Global_Weather_Thunder";
        public String soundCategory = "SFX";
        public float soundVolume = 1.0f;
        public float soundPitch = 1.0f;
        public String particleSystemId = "Lightning";
        public int lightningCount = 1;
        public float lightningSpread = 0.0f;
        public float cooldownSeconds = 1.0f;
        public boolean cancelInteraction = false;
        public String cooldownMessage = "Item is on cooldown.";
        public boolean parseMessages = true;
    }

    public static final class Hologram {
        public static final BuilderCodec<Hologram> CODEC = BuilderCodec.builder(Hologram.class, Hologram::new)
            .addField(new KeyedCodec<>("EntityId", Codec.STRING), (c, v) -> c.entityId = v, c -> c.entityId)
            .addField(new KeyedCodec<>("DefaultRotationYaw", Codec.FLOAT), (c, v) -> c.defaultRotationYaw = v, c -> c.defaultRotationYaw)
            .addField(new KeyedCodec<>("DefaultRotationPitch", Codec.FLOAT), (c, v) -> c.defaultRotationPitch = v, c -> c.defaultRotationPitch)
            .addField(new KeyedCodec<>("DefaultRotationRoll", Codec.FLOAT), (c, v) -> c.defaultRotationRoll = v, c -> c.defaultRotationRoll)
            .build();

        public String entityId = "AdminTools_Hologram";
        public float defaultRotationYaw = 0.0f;
        public float defaultRotationPitch = 0.0f;
        public float defaultRotationRoll = 0.0f;
    }

    public static final class Ui {
        public static final BuilderCodec<Ui> CODEC = BuilderCodec.builder(Ui.class, Ui::new)
            .addField(new KeyedCodec<>("UiPath", Codec.STRING), (c, v) -> c.uiPath = v, c -> c.uiPath)
            .addField(new KeyedCodec<>("Title", Codec.STRING), (c, v) -> c.title = v, c -> c.title)
            .addField(new KeyedCodec<>("Subtitle", Codec.STRING), (c, v) -> c.subtitle = v, c -> c.subtitle)
            .addField(new KeyedCodec<>("Body", Codec.STRING), (c, v) -> c.body = v, c -> c.body)
            .addField(new KeyedCodec<>("ParseMessages", Codec.BOOLEAN), (c, v) -> c.parseMessages = v, c -> c.parseMessages)
            .build();

        public String uiPath = "Pages/AdminToolsPage.ui";
        public String title = "Admin Tools";
        public String subtitle = "Configuration Panel";
        public String body = "Custom UI loaded from the plugin asset pack.";
        public boolean parseMessages = true;
    }
}

