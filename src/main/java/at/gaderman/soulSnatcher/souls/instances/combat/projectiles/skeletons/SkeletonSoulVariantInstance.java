package at.gaderman.soulSnatcher.souls.instances.combat.projectiles.skeletons;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.SkeletonShootGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityLaunchProjectileTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnProjectileHitTrigger;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SkeletonSoulVariantInstance extends SoulInstance implements OnEntityLaunchProjectileTrigger, OnProjectileHitTrigger {
    private final Options options;
    private final NamespacedKey shot_key;

    protected SkeletonSoulVariantInstance(LivingEntity carrier, SoulType soulType, Options options) {
        super(carrier, soulType);

        this.options = options;
        this.shot_key = new NamespacedKey(SoulSnatcher.getPlugin(), options.projectileKey);

        if (isInfused() && !(carrier instanceof AbstractSkeleton))
            Bukkit.getMobGoals().addGoal((Mob) carrier, 0, new SkeletonShootGoal((Mob) carrier, 6000));
    }

    @Override
    public void onEntityLaunchProjectile(LivingEntity carrier, Projectile projectile, ProjectileLaunchEvent event) {
        projectile.getPersistentDataContainer().set(shot_key, PersistentDataType.STRING, carrier.getUniqueId().toString());
        carrier.getWorld().playSound(carrier, options.shotSound, 1f, 0.2f);

        boolean isArrow = projectile instanceof AbstractArrow;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || !projectile.getPersistentDataContainer().has(shot_key)) {
                    cancel();
                    return;
                }

                if(isArrow && ((AbstractArrow) projectile).isInBlock()) return;

                projectile.getLocation().getWorld().spawnParticle(Particle.ENTITY_EFFECT, projectile.getLocation(),
                        4, 0, 0, 0, 0.2, options.effectColor);
            }
        }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, 3L);
    }

    @Override
    public void onProjectileHit(LivingEntity carrier, Projectile projectile, ProjectileHitEvent event) {
        if (!projectile.getPersistentDataContainer().has(shot_key)) return;

        if (!(event.getHitEntity() instanceof LivingEntity hit)) return;

        hit.addPotionEffect(new PotionEffect(options.effectType, options.effectDuration, options.effectAmplifier));
    }

    public record Options(String projectileKey, Color effectColor, Sound shotSound, PotionEffectType effectType,
                          int effectDuration, int effectAmplifier) { }
}
