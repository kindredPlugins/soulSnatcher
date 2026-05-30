package at.gaderman.soulSnatcher.souls.triggers;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Trigger when the soul carrier gets targeted by another entity
 */
public interface OnTargetTrigger {
    void onBeingTargeted(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event);
    void onCarrierTarget(LivingEntity carrier, LivingEntity target, EntityTargetLivingEntityEvent event);
}
