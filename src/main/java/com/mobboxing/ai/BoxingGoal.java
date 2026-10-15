package com.mobboxing.ai;

import com.mobboxing.BoxingConfig;
import com.mobboxing.BoxingHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * The boxing brain. An animal either
 *  - retaliates against whoever just hit it (another boxer, or a player), or
 *  - randomly picks a fight with a nearby boxer.
 * It then walks up and throws a punch every few ticks until someone is knocked out,
 * the round times out, or the opponent leaves.
 *
 * While the goal runs it keeps MobEntity#isAttacking() true; that flag is synced to clients,
 * which is how the renderer knows to stand the animal up and animate its front legs.
 */
public class BoxingGoal extends Goal {
    /** True only while one of our punches is being applied (read by the damage event). */
    private static boolean punching = false;

    public static boolean isPunching() {
        return punching;
    }

    private final AnimalEntity mob;
    private LivingEntity target;
    private int fightTicks;
    private int punchCooldown;
    private int nextFightAge;

    public BoxingGoal(AnimalEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private boolean validOpponent(LivingEntity other) {
        if (other == null || other == mob || !other.isAlive() || BoxingHelper.isKnockedOut(other)) {
            return false;
        }
        if (other instanceof PlayerEntity player) {
            return BoxingConfig.get().retaliateAgainstPlayers && !player.isCreative() && !player.isSpectator();
        }
        return other instanceof AnimalEntity animal && BoxingHelper.isBoxer(animal);
    }

    @Override
    public boolean canStart() {
        BoxingConfig cfg = BoxingConfig.get();
        if (!BoxingHelper.isBoxer(mob) || BoxingHelper.isKnockedOut(mob)) {
            return false;
        }

        // 1) Hit recently? Hit back.
        LivingEntity attacker = mob.getAttacker();
        if (attacker != null && mob.age - mob.getLastAttackedTime() < 100 && validOpponent(attacker)) {
            target = attacker;
            return true;
        }

        // 2) Occasionally start a fight with a neighbour.
        if (!cfg.randomFights || mob.age < nextFightAge || mob.getRandom().nextInt(Math.max(1, cfg.fightStartChance)) != 0) {
            return false;
        }
        Box area = mob.getBoundingBox().expand(cfg.searchRadius, 4.0, cfg.searchRadius);
        List<AnimalEntity> candidates = mob.getWorld().getEntitiesByClass(AnimalEntity.class, area, other ->
                other != mob
                        && validOpponent(other)
                        && (cfg.sameSpeciesFight || other.getType() != mob.getType())
                        && mob.getVisibilityCache().canSee(other));
        LivingEntity opponent = candidates.stream()
                .min(Comparator.comparingDouble(mob::squaredDistanceTo))
                .orElse(null);
        if (opponent == null) {
            return false;
        }
        target = opponent;
        // Squares the opponent up too: its own goal sees "I was just attacked by X" and fights back.
        opponent.setAttacker(mob);
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return target != null
                && validOpponent(target)
                && BoxingHelper.isBoxer(mob)
                && !BoxingHelper.isKnockedOut(mob)
                && fightTicks < BoxingConfig.get().maxFightTicks
                && mob.squaredDistanceTo(target) < 24.0 * 24.0;
    }

    @Override
    public void start() {
        fightTicks = 0;
        punchCooldown = 10; // short wind-up so the stance is visible before the first jab
        mob.setAttacking(true);
    }

    @Override
    public void stop() {
        mob.setAttacking(false);
        mob.getNavigation().stop();
        target = null;
        nextFightAge = mob.age + BoxingConfig.get().restTicks + mob.getRandom().nextInt(100);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        BoxingConfig cfg = BoxingConfig.get();
        fightTicks++;
        if (punchCooldown > 0) {
            punchCooldown--;
        }

        mob.getLookControl().lookAt(target, 30.0f, 30.0f);

        double reachSq = (mob.getWidth() * 2.0f) * (mob.getWidth() * 2.0f) + target.getWidth();
        double distSq = mob.squaredDistanceTo(target);

        if (distSq > reachSq * 0.6) {
            if (fightTicks % 4 == 0 || mob.getNavigation().isIdle()) {
                mob.getNavigation().startMovingTo(target, cfg.fightMoveSpeed);
            }
        } else {
            mob.getNavigation().stop(); // stand and trade blows
        }

        if (punchCooldown <= 0 && distSq <= reachSq) {
            punchCooldown = Math.max(4, cfg.punchCooldownTicks);
            mob.swingHand(Hand.MAIN_HAND);
            punching = true;
            try {
                target.damage(mob.getWorld().getDamageSources().mobAttack(mob), cfg.punchDamage);
            } finally {
                punching = false;
            }
            mob.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 0.8f,
                    0.9f + mob.getRandom().nextFloat() * 0.3f);
        }
    }
}
