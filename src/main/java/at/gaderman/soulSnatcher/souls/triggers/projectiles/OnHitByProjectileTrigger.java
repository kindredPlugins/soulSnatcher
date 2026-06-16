package at.gaderman.soulSnatcher.souls.triggers.projectiles;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;

public interface OnHitByProjectileTrigger {
    void onHitByProjectile(LivingEntity carrier, Projectile projectile, ProjectileHitEvent event);
}
