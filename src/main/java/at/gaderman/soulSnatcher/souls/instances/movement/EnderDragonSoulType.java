package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnEntityToggleGlideTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@AutoService(SoulType.class)
public class EnderDragonSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<EnderDragonSoulType> create(LivingEntity carrier) {
        return new EnderDragonSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "ender_dragon_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.ENDER_DRAGON;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "f68c1c079a7ffb36f48dd7150355e3e0b7f68dd605e6f8847313c360cf61e0c";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component displayName() {
        return ItemUtils.gradient("Ender Dragon Soul", TextColor.color(0x6c76ab), NamedTextColor.DARK_PURPLE);
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When gaining enough ")
                        .append(Component.text("elytra momentum ", NamedTextColor.AQUA)),
                Component.text("enter ")
                        .append(ItemUtils.gradient("Dragon Flight", NamedTextColor.DARK_AQUA, NamedTextColor.AQUA))
                        .append(Component.text(" which boosts your", NamedTextColor.WHITE)),
                Component.text("elytra fly speed and reduces friction."),
                Component.text("Flying through mobs damages and"),
                Component.text("knocks them away.")
        );
    }

    //region Config Values

    private static final String ENTER_DRAGON_FLIGHT_MAGNITUDE = "enter_dragon_flight_magnitude";
    private static final String INITIAL_BOOST = "initial_boost_power";
    private static final String CONTINUOUS_BOOST = "continuous_boost_power";
    private static final String ATTACK_MIN_MAGNITUDE = "attack_min_magnitude";
    private static final String FLY_THROUGH_DAMAGE = "fly_through_damage";
    private static final String FLY_THROUGH_KNOCK_MULTIPLIER = "fly_through_knock_multiplier";

    private final ConfigOption<Double> enterFlightMagnitude = configOption(ENTER_DRAGON_FLIGHT_MAGNITUDE, 1.1, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> initialBoost = configOption(INITIAL_BOOST, 1.5, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> continuousBoost = configOption(CONTINUOUS_BOOST, 1.005, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> attackMinMagnitude = configOption(ATTACK_MIN_MAGNITUDE, 1.3, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> flyThroughDamage = configOption(FLY_THROUGH_DAMAGE, 6.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> flyThroughKnockMultiplier = configOption(FLY_THROUGH_KNOCK_MULTIPLIER, 2.0, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                ENTER_DRAGON_FLIGHT_MAGNITUDE, "Amount of elytra velocity to enter dragon flight, measure in velocity vector magnitude squared (reference values at startup use: lunge III ~ 1.5 | firework rocket ~ 2.4 )",
                INITIAL_BOOST, "Multiplicator applied to the current velocity when dragon flight is activated (1.5 -> 150%)",
                CONTINUOUS_BOOST, "Continuous multiplier applied to elytra movement. CAREFUL !! changing this to a much higher value outgrows the elytra velocity falloff which can lead to infinite accelerating elytras",
                ATTACK_MIN_MAGNITUDE, "The min magnitude when mobs near the flying player are damaged and knocked away (visible by denser particles), 1.3 can be considered \"flying\" as values beneath it are rather fine adjustments",
                FLY_THROUGH_DAMAGE, "How much damage done to entities hit by the fly through",
                FLY_THROUGH_KNOCK_MULTIPLIER, "When hit by the fly through applies the direction of the attackers velocity times this multiplier, Y value is set independently"
        );
    }

    //endregion

    public static class EnderDragonSoulInstance extends SoulInstance<EnderDragonSoulType> implements OnEntityToggleGlideTrigger {
        protected EnderDragonSoulInstance(LivingEntity carrier, EnderDragonSoulType soulType) {
            super(carrier, soulType);
        }

        private ScheduledTask flyingTask;
        private boolean dragonFlight;

        private final Set<UUID> hitInFlight = new LinkedHashSet<>();

        @Override
        public void onToggleGlide(LivingEntity carrier, @NotNull EntityToggleGlideEvent event) {
            if (!event.isGliding() || flyingTask != null)
                return;

            flyingTask = carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), (task) -> {
                Location location = carrier.getLocation();

                if (!carrier.isGliding()) {
                    dragonFlight = false;
                    flyingTask = null;
                    task.cancel();

                    carrier.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.2f);
                    return;
                }

                double magnitude = carrier.getVelocity().lengthSquared();

                if (!dragonFlight) {
                    if (magnitude < soulType().enterFlightMagnitude.cached()) {
                        return;
                    }

                    dragonFlight = true;
                    carrier.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);

                    carrier.setVelocity(carrier.getVelocity().multiply(soulType().initialBoost.cached()));
                } else {
                    carrier.setVelocity(carrier.getVelocity().multiply(soulType().continuousBoost.cached()));
                }

                boolean highMagnitude = magnitude > soulType().attackMinMagnitude.cached();
                float offset = highMagnitude ? 2 : 0.8f;

                carrier.getWorld().spawnParticle(Particle.DRAGON_BREATH, location, highMagnitude ? 15 : 3,
                        offset, 1, offset, 0.2, 1f);

                if (Bukkit.getCurrentTick() % 10 == 0)
                    carrier.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);

                if (highMagnitude) {
                    applyDragonFlyHit(carrier);
                }
            }, () -> flyingTask = null, 1L, 1L);
        }

        private void applyDragonFlyHit(LivingEntity carrier) {
            Vector knockDirection = carrier.getLocation().getDirection().normalize().multiply(soulType().flyThroughKnockMultiplier.cached()).setY(1.3);

            Location loc = carrier.getLocation();
            Vector dir = loc.getDirection().setY(0).normalize();
            Vector normalVector = new Vector(-dir.getZ(), 0, dir.getX());

            double sideExtent = 3.0;
            double frontExtent = 1.0;
            double backExtent = 0.5;
            double upExtent = 2.5;
            double downExtent = 2.5;

            Vector base = loc.toVector();
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

            for (double f : new double[]{-backExtent, frontExtent}) {
                for (double s : new double[]{-sideExtent, sideExtent}) {
                    Vector corner = base.clone().add(dir.clone().multiply(f)).add(normalVector.clone().multiply(s));
                    minX = Math.min(minX, corner.getX());
                    maxX = Math.max(maxX, corner.getX());
                    minZ = Math.min(minZ, corner.getZ());
                    maxZ = Math.max(maxZ, corner.getZ());
                }
            }

            BoundingBox flyingBox = new BoundingBox(minX, base.getY() - downExtent, minZ,
                    maxX, base.getY() + upExtent, maxZ);

            carrier.getWorld().getNearbyEntities(flyingBox, target -> target instanceof LivingEntity
                    && !target.equals(carrier) && !hitInFlight.contains(target.getUniqueId())
            ).forEach(entity -> {
                LivingEntity target = (LivingEntity) entity;
                hitInFlight.add(target.getUniqueId());
                target.getScheduler().runDelayed(SoulSnatcher.getPlugin(),
                        _ -> hitInFlight.remove(target.getUniqueId()),
                        () -> hitInFlight.remove(target.getUniqueId()),
                        10);

                target.damage(soulType().flyThroughDamage.cached(), DamageSource.builder(DamageType.MOB_ATTACK)
                        .withDirectEntity(carrier)
                        .withCausingEntity(carrier)
                        .build());
                target.setVelocity(knockDirection);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1f, 1f);
            });
        }
    }
}