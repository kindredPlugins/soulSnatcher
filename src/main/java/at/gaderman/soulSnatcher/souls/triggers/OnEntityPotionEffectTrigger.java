package at.gaderman.soulSnatcher.souls.triggers;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;

public interface OnEntityPotionEffectTrigger {
    void onEntityPotionEffect(LivingEntity carrier, EntityPotionEffectEvent event);
}
