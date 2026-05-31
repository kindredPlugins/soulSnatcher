package at.gaderman.soulSnatcher.souls.triggers.damage;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public interface OnDamageDealtTrigger {
    void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event);
}
