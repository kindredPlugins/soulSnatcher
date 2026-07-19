package at.gaderman.soulSnatcher.souls.triggers.projectiles;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityExplodeEvent;

public interface OnProjectileExplosionTrigger {
    void onProjectileExplosion(LivingEntity carrier, Projectile projectile, EntityExplodeEvent event);
}
