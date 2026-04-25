package at.gaderman.soulSnatcher.souls.triggers.projectiles;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityShootBowEvent;

public interface OnEntityShootBowTrigger {
    void onEntityShootBow(LivingEntity carrier, EntityShootBowEvent event);
}
