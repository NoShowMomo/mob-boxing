package com.mobboxing.client;

import com.mobboxing.BoxingHelper;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client-only animation state for boxing animals (render thread only).
 * The renderer mixin advances it once per frame; the model mixins read it.
 */
public final class BoxerPose {
    public static final class State {
        /** 0 = on all fours, 1 = fully reared up. */
        public float stand;
        /** 0 = normal, 1 = lying knocked out. */
        public float ko;
        /** Punch progress 0..1 (vanilla swing animation). */
        public float swing;
        /** Which front limb throws the current punch. */
        public boolean leftNext;
        float lastSwing;
        float lastTime;
    }

    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    private BoxerPose() {
    }

    /** Advance and return the state, or null when the animal is in its normal pose. */
    public static State update(LivingEntity entity, float tickDelta) {
        if (!BoxingHelper.isEligibleType(entity) || !(entity instanceof MobEntity mob)) {
            return null;
        }
        State s = STATES.get(entity);
        boolean ko = BoxingHelper.isKnockedOut(entity);
        boolean fighting = !ko && mob.isAttacking();
        if (s == null) {
            if (!ko && !fighting) {
                return null;
            }
            s = new State();
            s.lastTime = entity.age + tickDelta;
            STATES.put(entity, s);
        }

        float now = entity.age + tickDelta;
        float dt = now - s.lastTime;
        if (dt < 0 || dt > 40) {
            dt = 0;
        }
        s.lastTime = now;

        s.stand = approach(s.stand, fighting ? 1f : 0f, dt / 6f);
        s.ko = approach(s.ko, ko ? 1f : 0f, dt / 5f);

        float swing = entity.getHandSwingProgress(tickDelta);
        if (swing > 0 && s.lastSwing == 0) {
            s.leftNext = !s.leftNext; // alternate left/right
        }
        s.lastSwing = swing;
        s.swing = swing;

        if (s.stand <= 0.001f && s.ko <= 0.001f) {
            STATES.remove(entity);
            return null;
        }
        return s;
    }

    public static State peek(LivingEntity entity) {
        return STATES.get(entity);
    }

    /**
     * Pose two "arms" (front legs or wings) as a boxer's guard, with the punching arm jabbing out.
     * Model-space convention: pitch 0 = limb hangs along the body's belly side; more negative = swung
     * toward the head. Once the body is tilted upright, -1.0 is a raised guard and -0.2 a straight jab.
     */
    public static void poseArms(State s, float ageInTicks, ModelPart rightArm, ModelPart leftArm) {
        float punch = MathHelper.sin(s.swing * (float) Math.PI);
        float bob = MathHelper.sin(ageInTicks * 0.25f) * 0.06f;
        float guard = -1.0f + bob;
        float jab = 0.8f * punch;

        float rightPitch = guard + (s.leftNext ? 0f : jab);
        float leftPitch = guard + (s.leftNext ? jab : 0f);

        rightArm.pitch = MathHelper.lerp(s.stand, rightArm.pitch, rightPitch);
        leftArm.pitch = MathHelper.lerp(s.stand, leftArm.pitch, leftPitch);
    }

    private static float approach(float value, float target, float step) {
        if (value < target) {
            return Math.min(target, value + step);
        }
        return Math.max(target, value - step);
    }
}
