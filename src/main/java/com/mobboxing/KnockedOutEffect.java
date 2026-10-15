package com.mobboxing;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

/** Applied to a beaten boxer: it can't move and (on the client) lies on its side. */
public class KnockedOutEffect extends StatusEffect {
    public KnockedOutEffect() {
        super(StatusEffectCategory.HARMFUL, 0xFFD23F);
        this.addAttributeModifier(
                EntityAttributes.GENERIC_MOVEMENT_SPEED,
                Identifier.of(MobBoxing.MOD_ID, "knocked_out"),
                -1.0,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
