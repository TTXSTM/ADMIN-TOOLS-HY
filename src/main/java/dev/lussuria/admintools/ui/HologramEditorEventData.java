package dev.lussuria.admintools.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class HologramEditorEventData {
    static final String KEY_ACTION = "Action";
    static final String KEY_TEXT = "@InputText";

    public static final BuilderCodec<HologramEditorEventData> CODEC = BuilderCodec.builder(HologramEditorEventData.class, HologramEditorEventData::new)
        .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (c, v) -> c.action = v, c -> c.action)
        .addField(new KeyedCodec<>(KEY_TEXT, Codec.STRING), (c, v) -> c.text = v, c -> c.text)
        .build();

    private String action;
    private String text;

    public String getAction() {
        return action;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "HologramEditorEventData{action=" + action + ", text=" + text + "}";
    }
}
