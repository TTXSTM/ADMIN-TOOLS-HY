package dev.lussuria.admintools.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class AdminToolsPageEventData {
    static final String KEY_COMMAND = "Command";
    static final String KEY_TEXT = "@CommandText";

    public static final BuilderCodec<AdminToolsPageEventData> CODEC = BuilderCodec.builder(AdminToolsPageEventData.class, AdminToolsPageEventData::new)
        .addField(new KeyedCodec<>(KEY_COMMAND, Codec.STRING), (c, v) -> c.command = v, c -> c.command)
        .addField(new KeyedCodec<>(KEY_TEXT, Codec.STRING), (c, v) -> c.commandText = v, c -> c.commandText)
        .build();

    private String command;
    private String commandText;

    public String getCommand() {
        return command;
    }

    public String getCommandText() {
        return commandText;
    }
}
