package dev.lussuria.admintools.hologram;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public final class HologramSpawner {
    private static final Vector3f ZERO_ROTATION = new Vector3f(0f, 0f, 0f);

    private HologramSpawner() {
    }

    /**
     * Spawn a single hologram line entity at the given position.
     * Must be called on the world thread.
     * Matches HydroHologram's exact spawning recipe for text lines.
     */
    public static Ref<EntityStore> spawnLine(World world, Vector3d position, String text, float scale) {
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // 1. TransformComponent - position + zero rotation
        holder.putComponent(TransformComponent.getComponentType(),
            new TransformComponent(position, ZERO_ROTATION));

        // 2. ProjectileComponent with "Projectile" archetype (CRITICAL for visibility)
        ProjectileComponent projComp = new ProjectileComponent("Projectile");
        holder.putComponent(ProjectileComponent.getComponentType(), projComp);
        if (projComp.getProjectile() == null) {
            projComp.initialize();
        }

        // 3. Intangible marker
        holder.ensureComponent(Intangible.getComponentType());

        // 4. Nameplate with text
        holder.addComponent(Nameplate.getComponentType(), new Nameplate(text));

        // 5. EntityScaleComponent
        holder.addComponent(EntityScaleComponent.getComponentType(),
            new EntityScaleComponent(scale));

        // 6. NetworkId (required for client visibility)
        holder.addComponent(NetworkId.getComponentType(),
            new NetworkId(entityStore.takeNextNetworkId()));

        // 7. UUIDComponent
        holder.addComponent(UUIDComponent.getComponentType(),
            new UUIDComponent(UUID.randomUUID()));

        // 8. NonSerialized marker
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        // Spawn entity
        return store.addEntity(holder, AddReason.SPAWN);
    }

    /**
     * Remove a hologram line entity.
     * Must be called on the world thread.
     */
    public static void removeLine(Ref<EntityStore> ref) {
        if (ref != null && ref.isValid()) {
            ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
        }
    }
}
