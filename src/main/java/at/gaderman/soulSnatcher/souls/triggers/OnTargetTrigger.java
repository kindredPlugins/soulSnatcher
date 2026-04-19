package at.gaderman.soulSnatcher.souls.triggers;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Trigger when the soul carrier gets targeted by another entity
 */
public interface OnTargetTrigger {
    void onTarget(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event);
}
