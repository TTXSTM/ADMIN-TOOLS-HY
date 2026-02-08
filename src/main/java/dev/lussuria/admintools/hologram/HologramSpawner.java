package dev.lussuria.admintools.hologram;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
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
     */
    public static Ref<EntityStore> spawnLine(World world, Vector3d position, String text, float scale) {
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.putComponent(TransformComponent.getComponentType(),
            new TransformComponent(position, ZERO_ROTATION));

        holder.ensureComponent(ProjectileComponent.getComponentType());

        holder.putComponent(Intangible.getComponentType(), Intangible.INSTANCE);

        // Required for the nameplate tracker update (NameplateSystems) to publish changes to clients.
        holder.ensureComponent(EntityModule.get().getVisibleComponentType());

        holder.addComponent(Nameplate.getComponentType(), new Nameplate(text));

        if (scale > 0f) {
            holder.addComponent(EntityScaleComponent.getComponentType(),
                new EntityScaleComponent(scale));
        }

        holder.addComponent(NetworkId.getComponentType(),
            new NetworkId(entityStore.takeNextNetworkId()));

        UUID entityUuid = UUID.randomUUID();
        holder.addComponent(UUIDComponent.getComponentType(),
            new UUIDComponent(entityUuid));

        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
        return ref;
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
