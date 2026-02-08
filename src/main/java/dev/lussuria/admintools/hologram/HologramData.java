package dev.lussuria.admintools.hologram;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HologramData {
    private UUID id;
    private String name;
    private double posX;
    private double posY;
    private double posZ;
    private String worldId;
    private final List<String> lines;
    private UUID creatorId;
    private double lineSpacing;

    private transient final List<Ref<EntityStore>> lineEntityRefs = new ArrayList<>();
    private transient boolean spawned;

    public HologramData() {
        this.id = UUID.randomUUID();
        this.name = "";
        this.lines = new ArrayList<>();
        this.lineSpacing = 0.25;
    }

    public HologramData(String name, double posX, double posY, double posZ, String worldId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.worldId = worldId;
        this.lines = new ArrayList<>();
        this.lineSpacing = 0.25;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public String getWorldId() {
        return worldId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public List<String> getLines() {
        return lines;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    public double getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public double getLinePositionY(int lineIndex) {
        return posY + (-lineIndex * lineSpacing);
    }

    public List<Ref<EntityStore>> getLineEntityRefs() {
        return lineEntityRefs;
    }

    public void clearLineEntityRefs() {
        lineEntityRefs.clear();
    }

    public boolean isSpawned() {
        return spawned;
    }

    public void setSpawned(boolean spawned) {
        this.spawned = spawned;
    }
}
