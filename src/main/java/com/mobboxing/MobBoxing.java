package com.mobboxing;

import com.mobboxing.ai.BoxingGoal;
import com.mobboxing.mixin.MobEntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobBoxing implements ModInitializer {
    public static final String MOD_ID = "mobboxing";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static RegistryEntry<StatusEffect> KNOCKED_OUT;

    @Override
    public void onInitialize() {
        BoxingConfig.load();

        KNOCKED_OUT = Registry.registerReference(
                Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "knocked_out"), new KnockedOutEffect());

        // Give every animal the boxing goal when it enters the world.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof AnimalEntity animal) {
                attachGoal(animal);
            }
        });

        // Boxing punches between animals can't kill: they knock the victim out instead.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(MobBoxing::allowDamage);

        LOGGER.info("Mob Boxing loaded - ding ding!");
    }

    private static void attachGoal(AnimalEntity animal) {
        GoalSelector selector = ((MobEntityAccessor) animal).mobboxing$getGoalSelector();
        for (PrioritizedGoal pg : selector.getGoals()) {
            if (pg.getGoal() instanceof BoxingGoal) {
                return; // already attached (e.g. entity re-added after a dimension change)
            }
        }
        // Priority 0 beats the vanilla "run away when hit" goal (priority 1 on cows, pigs, sheep, ...).
        selector.add(0, new BoxingGoal(animal));
    }

    private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!BoxingGoal.isPunching()) {
            return true; // only our own punches are non-lethal; everything else is vanilla
        }
        if (!(victim instanceof AnimalEntity animal) || !BoxingHelper.isEligibleType(animal)) {
            return true; // players etc. take punches normally
        }
        if (BoxingHelper.isKnockedOut(animal)) {
            return false; // no hitting a downed fighter
        }
        if (animal.getHealth() - amount <= BoxingConfig.get().knockoutHealth) {
            knockOut(animal);
            return false;
        }
        return true;
    }

    private static void knockOut(AnimalEntity victim) {
        BoxingConfig cfg = BoxingConfig.get();
        victim.addStatusEffect(new StatusEffectInstance(KNOCKED_OUT, cfg.knockoutTicks, 0, false, false, true));
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, cfg.knockoutTicks, 1, false, false, false));
        victim.setAttacker(null);
        victim.getNavigation().stop();

        if (victim.getWorld() instanceof ServerWorld world) {
            world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.BLOCK_BELL_USE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1.0f, 0.8f);
            world.spawnParticles(ParticleTypes.CRIT,
                    victim.getX(), victim.getY() + victim.getHeight() * 0.8, victim.getZ(),
                    16, 0.4, 0.3, 0.4, 0.3);
        }
    }
}
