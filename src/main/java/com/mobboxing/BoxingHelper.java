package com.mobboxing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BoxingHelper {
    private static final Map<EntityType<?>, Boolean> TYPE_CACHE = new ConcurrentHashMap<>();

    private BoxingHelper() {
    }

    static void clearCache() {
        TYPE_CACHE.clear();
    }

    /** Is this kind of entity allowed to box at all (ignores per-animal state)? Safe on both sides. */
    public static boolean isEligibleType(Entity entity) {
        if (!(entity instanceof PassiveEntity)) {
            return false;
        }
        return TYPE_CACHE.computeIfAbsent(entity.getType(), type ->
                !BoxingConfig.get().excludedEntities.contains(Registries.ENTITY_TYPE.getId(type).toString()));
    }

    /** Can this particular animal box right now? */
    public static boolean isBoxer(PassiveEntity animal) {
        return BoxingConfig.get().enabled
                && isEligibleType(animal)
                && !animal.isBaby()
                && !animal.hasVehicle()
                && !animal.hasPassengers()
                && !animal.isLeashed()
                && !(animal instanceof TameableEntity pet && pet.isTamed())
                && !(animal instanceof AbstractHorseEntity horse && horse.isTame());
    }

    public static boolean isKnockedOut(net.minecraft.entity.LivingEntity entity) {
        return entity.hasStatusEffect(MobBoxing.KNOCKED_OUT);
    }
}
