package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.config.ConfigOption;
import at.gaderman.soulSnatcher.mobGoals.custom.RabbitJumpGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnPlayerJumpTrigger;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.auto.service.AutoService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class RabbitSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<RabbitSoulType> create(LivingEntity carrier) {
        return new RabbitSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "rabbit_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.RABBIT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "7d1169b2694a6aba826360992365bcda5a10c89a3aa2b48c438531dd8685c3a7";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component defaultDisplayName() {
        return Component.text("Rabbit", TextColor.color(0x796954));
    }

    @Override
    public @NotNull List<Component> defaultDescription() {
        return List.of(
                Component.text("Consecutive ")
                        .append(Component.text("sprint jumps ", NamedTextColor.GOLD))
                        .append(Component.text(" will get")),
                Component.text("increasingly more horizontal force.")
        );
    }

    //region Config Values

    private static final String BASE_BOOST_CONFIG_ID = "base_jump_boost";
    private static final String BOOST_PER_JUMP_CONFIG_ID = "boost_per_jump";
    private static final String MAX_CHAIN_BOOST_CONFIG_ID = "max_chain_boost";
    private static final String VERTICAL_BOOST_CONFIG_ID = "vertical_boost";
    private static final String EXHAUSTION_USE_CONFIG_ID = "exhaustion_use";

    private final ConfigOption<Double> baseBoost = configOption(BASE_BOOST_CONFIG_ID, 0.1, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> boostPerJump = configOption(BOOST_PER_JUMP_CONFIG_ID, 0.05, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> maxChainBoost = configOption(MAX_CHAIN_BOOST_CONFIG_ID, 0.4, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> verticalBoost = configOption(VERTICAL_BOOST_CONFIG_ID, 0.3, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> exhaustionUse = configOption(EXHAUSTION_USE_CONFIG_ID, 0.3, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                BASE_BOOST_CONFIG_ID, "Base amount of horizontal boost applied on a consecutive jump as xz value",
                BOOST_PER_JUMP_CONFIG_ID, "Amount that gets added to the base amount for each consecutive jump",
                MAX_CHAIN_BOOST_CONFIG_ID, "Max amount the xz value of the boost vector can reach",
                VERTICAL_BOOST_CONFIG_ID, "The fixed vertical boost a consecutive jump receives",
                EXHAUSTION_USE_CONFIG_ID, "Amount of exhaustion added per consecutive jump, default value aims at adding meaningful hunger consumption (2.75 would be around 1 hunger point loss)"
        );
    }

    //endregion

    public final static int MAX_KYOTE_TICKS = 3;

    public static class RabbitSoulInstance extends SoulInstance<RabbitSoulType> implements OnPlayerJumpTrigger {

        private boolean chainActive;
        private int consecutiveJumpCount;
        private int groundedTicks;
        private ScheduledTask chainTask;

        private static final long JUMP_MIN_INTERVAL = 250_000_000L;
        private long lastJump;

        protected RabbitSoulInstance(LivingEntity carrier, RabbitSoulType soulType) {
            super(carrier, soulType);

            if (carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new RabbitJumpGoal(mob, this));
        }

        @Override
        public void onPlayerJump(Player carrier, PlayerJumpEvent event) {
            groundedTicks = 0;

            if(lastJump > System.nanoTime() - JUMP_MIN_INTERVAL)
                return;

            lastJump = System.nanoTime();

            if (!chainActive || !carrier.isSprinting() || carrier.isSneaking()) {
                consecutiveJumpCount = 0;
                chainActive = true;
                startChainTask(carrier);
                return;
            }

            consecutiveJumpCount++;
            boost(carrier);
        }

        public void boost(LivingEntity carrier) {
            double strength = soulType().baseBoost.cached() + Math.min(consecutiveJumpCount * soulType().boostPerJump.cached(), soulType().maxChainBoost.cached());

            Location loc = carrier.getLocation();
            Vector force = loc.getDirection().setY(0);
            if (force.lengthSquared() > 1.0E-4)
                force.normalize().multiply(strength);
            else
                force.zero();
            force.setY(soulType().verticalBoost.cached());

            carrier.setVelocity(carrier.getVelocity().add(force));
            if (carrier instanceof Player player)
                player.setExhaustion(player.getExhaustion() + soulType().exhaustionUse.cached().floatValue());

            carrier.getWorld().playSound(loc, Sound.ENTITY_RABBIT_ATTACK, 0.4f, Math.max(1.4f + consecutiveJumpCount * 0.1f, 2f));
            Particle.CLOUD.builder()
                    .location(loc)
                    .count(4 + consecutiveJumpCount * 2)
                    .offset(0.2, 0.1, 0.2)
                    .receivers(8)
                    .speed(0)
                    .spawn();
        }

        private void startChainTask(Player carrier) {
            if (chainTask != null)
                return;

            chainTask = carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), task -> {
                if (!carrier.isOnGround())
                    return;

                if (++groundedTicks > MAX_KYOTE_TICKS)
                    endChain();
            }, this::resetState, 1, 1);
        }

        private void endChain() {
            if (chainTask != null) {
                chainTask.cancel();
            }
            resetState();
        }

        private void resetState() {
            chainTask = null;
            chainActive = false;
            consecutiveJumpCount = 0;
            groundedTicks = 0;
        }

        @Override
        protected void cleanUp() {
            super.cleanUp();
            endChain();
        }
    }
}
