package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.config.ConfigOption;
import at.gaderman.soulSnatcher.mobGoals.ability.SlimeJumpGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import com.google.auto.service.AutoService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class SlimeSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<SlimeSoulType> create(LivingEntity carrier) {
        return new SlimeSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "slime_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SLIME;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "68d9c023736cbbc979164ae8201bb491a9146e2791a0f14bd6f4bc276bf84c79";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component defaultDisplayName() {
        return Component.text("Slime", TextColor.color(0x69ad58));
    }

    @Override
    public @NotNull List<Component> defaultDescription() {
        return List.of(
                Component.text("Makes all ground behave like a ")
                        .append(Component.text("slime block", TextColor.color(0x69ad58)))
                        .append(Component.text("."))
        );
    }

    //region Config Values

    private static final String MIN_IMPACT_CONFIG_ID = "min_impact";
    private static final String BOUNCE_FACTOR_CONFIG_ID = "bounce_factor";
    private static final String MAX_BOUNCE_CONFIG_ID = "max_bounce";

    private final ConfigOption<Double> minImpact = configOption(MIN_IMPACT_CONFIG_ID, 0.5, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> bounceFactor = configOption(BOUNCE_FACTOR_CONFIG_ID, 0.85, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> maxBounce = configOption(MAX_BOUNCE_CONFIG_ID, 2.5, FileConfiguration::getDouble, value -> Math.clamp(value, 0, 8));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                MIN_IMPACT_CONFIG_ID, "Amount of fall impact needed to trigger the bounce, 0.5 -> includes heights higher than a default jump",
                BOUNCE_FACTOR_CONFIG_ID, "Per cent of how much fall impact is translated into bounce back velocity as y-value of the vector (0.85 -> 85%)",
                MAX_BOUNCE_CONFIG_ID, "The maximum y the bounce vector can reach, default slime blocks use an exponential function, this ability just has a cap"
        );
    }

    //endregion

    public static class SlimeSoulInstance extends SoulInstance<SlimeSoulType> implements OnDamageReceivedTrigger {

        private final ScheduledTask bounceTask;
        private Location savedLocation;
        private double lastDescent;

        protected SlimeSoulInstance(LivingEntity carrier, SlimeSoulType soulType) {
            super(carrier, soulType);

            this.savedLocation = carrier.getLocation();

            if(carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new SlimeJumpGoal(mob));

            bounceTask = carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), _ -> {
                Location location = carrier.getLocation();

                if (savedLocation == null || !location.getWorld().equals(savedLocation.getWorld()) || location.distanceSquared(savedLocation) > 64) {
                    this.savedLocation = location;
                    this.lastDescent = 0;
                    return;
                }

                double dx = location.getX() - savedLocation.getX();
                double dz = location.getZ() - savedLocation.getZ();
                double descent = savedLocation.getY() - location.getY();
                this.savedLocation = location;

                boolean grounded = carrier.isOnGround();
                double impact = Math.max(descent, lastDescent);

                if (grounded && !carrier.isSneaking() && impact > soulType.minImpact.cached()) {
                    bounce(dx, dz, impact);
                    lastDescent = 0;
                    return;
                }

                lastDescent = grounded ? 0 : Math.max(descent, 0);
            }, null, 1, 1);
        }

        @Override
        protected void cleanUp() {
            super.cleanUp();

            if (bounceTask != null)
                bounceTask.cancel();
        }

        private void bounce(double dx, double dz, double impact) {
            LivingEntity carrier = carrier();
            double up = Math.min(impact * soulType().bounceFactor.cached(), soulType().maxBounce.cached());

            if (carrier instanceof Player player && player.getCurrentInput().isJump())
                up = 0.5;

            carrier.setFallDistance(0);
            carrier.setVelocity(new Vector(dx, up, dz));

            float strength = (float) Math.min(1.0, impact);
            carrier.getWorld().playSound(carrier.getLocation(), Sound.BLOCK_SLIME_BLOCK_HIT, strength, 1f);
            Particle.ITEM_SLIME.builder()
                    .location(carrier.getLocation())
                    .offset(0.5, 0.2, 0.5)
                    .count((int) (10 + 30 * strength))
                    .receivers(16)
                    .spawn();
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
                return;

            if (carrier instanceof Player player && player.isSneaking())
                return;

            event.setCancelled(true);
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
        }
    }
}
