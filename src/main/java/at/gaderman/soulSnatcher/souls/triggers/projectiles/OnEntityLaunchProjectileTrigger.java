package at.gaderman.soulSnatcher.souls.triggers.projectiles;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public interface OnEntityLaunchProjectileTrigger {
    void onEntityLaunchProjectile(LivingEntity carrier, Projectile projectile, ProjectileLaunchEvent event);
}
