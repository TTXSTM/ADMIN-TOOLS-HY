package dev.lussuria.admintools.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class HologramEditorEventData {
    static final String KEY_ACTION = "Action";
    static final String KEY_LINE_INDEX = "LineIndex";
    static final String KEY_NEW_LINE_INPUT = "@NewLineInput";
    static final String KEY_POS_X = "@XInput";
    static final String KEY_POS_Y = "@YInput";
    static final String KEY_POS_Z = "@ZInput";

    public static final BuilderCodec<HologramEditorEventData> CODEC = BuilderCodec.builder(HologramEditorEventData.class, HologramEditorEventData::new)
        .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (c, v) -> c.action = v, c -> c.action)
        .addField(new KeyedCodec<>(KEY_LINE_INDEX, Codec.STRING), (c, v) -> c.lineIndex = v, c -> c.lineIndex)
        .addField(new KeyedCodec<>(KEY_NEW_LINE_INPUT, Codec.STRING), (c, v) -> c.newLineInput = v, c -> c.newLineInput)
        .addField(new KeyedCodec<>(KEY_POS_X, Codec.STRING), (c, v) -> c.posX = v, c -> c.posX)
        .addField(new KeyedCodec<>(KEY_POS_Y, Codec.STRING), (c, v) -> c.posY = v, c -> c.posY)
        .addField(new KeyedCodec<>(KEY_POS_Z, Codec.STRING), (c, v) -> c.posZ = v, c -> c.posZ)
        .build();

    private String action;
    private String lineIndex;
    private String newLineInput;
    private String posX;
    private String posY;
    private String posZ;

    public String getAction() {
        return action;
    }

    public String getLineIndex() {
        return lineIndex;
    }

    public int getLineIndexInt() {
        try {
            return Integer.parseInt(lineIndex);
        } catch (Exception e) {
            return -1;
        }
    }

    public String getNewLineInput() {
        return newLineInput;
    }

    public double getPosXDouble(double fallback) {
        try {
            return Double.parseDouble(posX);
        } catch (Exception e) {
            return fallback;
        }
    }

    public double getPosYDouble(double fallback) {
        try {
            return Double.parseDouble(posY);
        } catch (Exception e) {
            return fallback;
        }
    }

    public double getPosZDouble(double fallback) {
        try {
            return Double.parseDouble(posZ);
        } catch (Exception e) {
            return fallback;
        }
    }
}
