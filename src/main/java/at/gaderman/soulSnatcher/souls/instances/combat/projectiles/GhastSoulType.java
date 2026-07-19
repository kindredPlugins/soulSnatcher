package at.gaderman.soulSnatcher.souls.instances.combat.projectiles;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.GhastShootGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityLaunchProjectileTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnProjectileExplosionTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnProjectileHitTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.destroystokyo.paper.entity.RangedEntity;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class GhastSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<GhastSoulType> create(LivingEntity carrier) {
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
        double cooldown = Math.round(ghastShotCooldown.cached() / 10.0) / 100.0;
        boolean isInt = Math.ceil(cooldown) == cooldown;

        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Every ")
                        .append(Component.text((isInt ? (int) (cooldown) : cooldown) + "s ", NamedTextColor.GOLD))
                        .append(Component.text("your next shot projectile")),
                Component.text("will explode upon impact, dealing"),
                Component.text("knockback and up to ")
                        .append(Component.text(maxAoeDamage.cached() + " ", NamedTextColor.RED))
                        .append(Component.text("AOE damage."))
        );
    }

    private static final String GHAST_SHOT_COOLDOWN_CONFIG_ID = "ghast_shot_cooldown";
    private static final String AOE_RADIUS_CONFIG_ID = "aoe_radius";
    private static final String MAX_DAMAGE_CONFIG_ID = "max_AoE_damage";
    private static final String MIN_DAMAGE_CONFIG_ID = "min_AoE_damage";
    private static final String PROJECTILE_SPEED_MULTIPLIER_CONFIG_ID = "projectile_speed_multiplier";

    private final ConfigOption<Integer> ghastShotCooldown = configOption(GHAST_SHOT_COOLDOWN_CONFIG_ID, 1500, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Double> aoeRadius = configOption(AOE_RADIUS_CONFIG_ID, 3.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> maxAoeDamage = configOption(MAX_DAMAGE_CONFIG_ID, 6.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> minAoeDamage = configOption(MIN_DAMAGE_CONFIG_ID, 2.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> projectileSpeedMultiplier = configOption(PROJECTILE_SPEED_MULTIPLIER_CONFIG_ID, 0.9, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                GHAST_SHOT_COOLDOWN_CONFIG_ID, "Cooldown between each projectile fired gains ghast shot attribute in ms (1000ms = 1s)",
                AOE_RADIUS_CONFIG_ID, "Radius of how large the resulting AoE explosion from a ghast shot is",
                MAX_DAMAGE_CONFIG_ID, "Maximum amount of damage dealt by the AoE explosion when hit at the center",
                MIN_DAMAGE_CONFIG_ID, "Minimum amount of damage dealt by the AoE explosion",
                PROJECTILE_SPEED_MULTIPLIER_CONFIG_ID, "Multiplier of projectile speed of ghast shots (1 means no change in speed)"
        );
    }

    public static class GhastSoulInstance extends SoulInstance<GhastSoulType> implements OnEntityLaunchProjectileTrigger, OnProjectileHitTrigger, OnProjectileExplosionTrigger {
        protected GhastSoulInstance(LivingEntity carrier, GhastSoulType soulType) {
            super(carrier, soulType);

            if (isInfused() && !(carrier instanceof RangedEntity))
                Bukkit.getMobGoals().addGoal((Mob) carrier, 0, new GhastShootGoal((Mob) carrier, 5000));
        }

        private long lastGhastShot;

        private static final NamespacedKey GHAST_SHOT = new NamespacedKey(SoulSnatcher.getPlugin(), "ghast_shot");

        @Override
        public void onEntityLaunchProjectile(LivingEntity carrier, Projectile projectile, ProjectileLaunchEvent event) {
            if (lastGhastShot > System.currentTimeMillis() - soulType().ghastShotCooldown.cached()) return;

            lastGhastShot = System.currentTimeMillis();

            projectile.getPersistentDataContainer().set(GHAST_SHOT, PersistentDataType.STRING, carrier.getUniqueId().toString());
            carrier.getWorld().playSound(carrier, Sound.ENTITY_GHAST_SHOOT, 1f, 0.5f);

            projectile.setVelocity(projectile.getVelocity().multiply(soulType().projectileSpeedMultiplier.cached()));

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

        @Override
        public void onProjectileHit(LivingEntity carrier, Projectile projectile, ProjectileHitEvent event) {
            if (!projectile.getPersistentDataContainer().has(GHAST_SHOT)) return;

            projectile.getPersistentDataContainer().remove(GHAST_SHOT);

            Location location = projectile.getLocation();
            double radius = soulType().aoeRadius.cached();
            projectile.getNearbyEntities(radius, radius, radius).forEach(entity -> {
                if (!(entity instanceof LivingEntity hit)) return;

                double distance = entity.getLocation().distance(location);
                double damage = Math.max(soulType().minAoeDamage.cached(), soulType().maxAoeDamage.cached() - (distance * 1.5));
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

        @Override
        public void onProjectileExplosion(LivingEntity carrier, Projectile projectile, EntityExplodeEvent event) {
            if(!(projectile instanceof LargeFireball)) return;

            event.blockList().clear();
        }
    }
}
