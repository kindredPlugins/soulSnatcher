package at.gaderman.soulSnatcher.souls.instances.combat.projectiles;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.GhastShootGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityLaunchProjectileTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnProjectileHitTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class GhastSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new GhastSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "ghast_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.GHAST;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "504843421c218d0634455fdb1a6c5f7ae5b85098a50b12b9ed9d9310c84dc61b";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Ghast Soul", TextColor.color(0xd5cccc));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }

    public static class GhastSoulInstance extends SoulInstance implements OnEntityLaunchProjectileTrigger, OnProjectileHitTrigger {
        protected GhastSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);

            if (isInfused() && !(carrier instanceof AbstractSkeleton))
                Bukkit.getMobGoals().addGoal((Mob) carrier, 0, new GhastShootGoal((Mob) carrier, 5000));
        }

        private long lastGhastShot;
        private static final long GHAST_SHOT_COOLDOWN = 1500;

        private static final NamespacedKey GHAST_SHOT = new NamespacedKey(SoulSnatcher.getPlugin(), "ghast_shot");

        @Override
        public void onEntityLaunchProjectile(LivingEntity carrier, Projectile projectile, ProjectileLaunchEvent event) {
            if (lastGhastShot > System.currentTimeMillis() - GHAST_SHOT_COOLDOWN) return;

            lastGhastShot = System.currentTimeMillis();

            projectile.getPersistentDataContainer().set(GHAST_SHOT, PersistentDataType.STRING, carrier.getUniqueId().toString());
            carrier.getWorld().playSound(carrier, Sound.ENTITY_GHAST_SHOOT, 1f, 0.5f);

            projectile.setVelocity(projectile.getVelocity().multiply(PROJECTILE_SPEED_MULTIPLIER));

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!projectile.isValid() || !projectile.getPersistentDataContainer().has(GHAST_SHOT)) {
                        cancel();
                        return;
                    }

                    projectile.getLocation().getWorld().spawnParticle(Particle.LAVA, projectile.getLocation(), 2, 0, 0, 0, 0.2);
                }
            }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, 3L);
        }

        private static final double PROJECTILE_SPEED_MULTIPLIER = 0.9;
        private static final double MAX_DAMAGE = 6;
        private static final double MIN_DAMAGE = 2;

        @Override
        public void onProjectileHit(LivingEntity carrier, Projectile projectile, ProjectileHitEvent event) {
            if (!projectile.getPersistentDataContainer().has(GHAST_SHOT)) return;

            projectile.getPersistentDataContainer().remove(GHAST_SHOT);

            Location location = projectile.getLocation();
            projectile.getNearbyEntities(3, 3, 3).forEach(entity -> {
                if (!(entity instanceof LivingEntity hit)) return;
                double distance = entity.getLocation().distance(location);
                double damage = Math.max(MIN_DAMAGE, MAX_DAMAGE - (distance * 1.5));
                hit.damage(damage, DamageSource.builder(DamageType.EXPLOSION)
                        .withCausingEntity(carrier)
                        .withDirectEntity(projectile)
                        .withDamageLocation(location)
                        .build());

                Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () ->
                                hit.setVelocity(hit.getLocation().toVector().subtract(location.toVector())
                                        .setY(0)
                                        .normalize()
                                        .multiply(0.75)
                                        .setY(0.5)),
                        1L);
            });

            location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1);
            location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
        }
    }
}
