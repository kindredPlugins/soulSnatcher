package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.PhantomAttackGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSneakToggleTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class PhantomSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<PhantomSoulType> create(LivingEntity carrier) {
        return new PhantomSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "phantom_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.PHANTOM;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "7e95153ec23284b283f00d19d29756f244313a061b70ac03b97d236ee57bd982";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Phantom Soul", TextColor.color(0x5061a4));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("While falling downwards, hold ")
                        .append(Component.keybind("key.sneak", NamedTextColor.GOLD)),
                Component.text("to begin ")
                        .append(Component.text("gliding ", NamedTextColor.AQUA))
                        .append(Component.text("downwards, steadily", NamedTextColor.WHITE)),
                Component.text("reducing your downwards momentum.")
        );
    }

    //region Config Values

    private static final String DECAY_K_CONFIG_ID = "glide_decay_k";
    private static final String MIN_DOWNWARD_CONFIG_ID = "glide_min_downward";
    private static final String SMOOTH_CONFIG_ID = "glide_smooth";
    private static final String FORWARD_MULTIPLIER_CONFIG_ID = "glide_forward_multiplier";

    private final ConfigOption<Double> decayK = configOption(DECAY_K_CONFIG_ID, 0.06, FileConfiguration::getDouble);
    private final ConfigOption<Double> minDownward = configOption(MIN_DOWNWARD_CONFIG_ID, 0.1, FileConfiguration::getDouble);
    private final ConfigOption<Double> smooth = configOption(SMOOTH_CONFIG_ID, 0.18, FileConfiguration::getDouble);
    private final ConfigOption<Double> forwardMultiplier = configOption(FORWARD_MULTIPLIER_CONFIG_ID, 0.05, FileConfiguration::getDouble);

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                DECAY_K_CONFIG_ID, "The k factor which describes the gliding curve based on the initial downward speed: f(t) = <minDownward> + (1 - <minDownward>) * exp(-k * t)",
                MIN_DOWNWARD_CONFIG_ID, "The asymptote to the gliding curve, downwards speed will gradually near this speed",
                SMOOTH_CONFIG_ID, "Smoothes out the gliding curve following the function: smoothedY = currentY * (1 - smoothFactor) + y * smoothFactor",
                FORWARD_MULTIPLIER_CONFIG_ID, "In addition to the gliding gives a slight boost in facing direction using this force"
        );
    }

    //endregion

    public static class PhantomSoulInstance extends SoulInstance<PhantomSoulType> implements OnSneakToggleTrigger {
        protected PhantomSoulInstance(LivingEntity carrier, PhantomSoulType soulType) {
            super(carrier, soulType);

            if (carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new PhantomAttackGoal(mob, this));
        }

        private boolean gliding;

        public void activateGliding() {
            gliding = true;
            LivingEntity carrier = carrier();

            Vector velocity = carrier.getVelocity();
            if (velocity.getY() < 0) carrier.setVelocity(velocity.setY(velocity.getY() * 0.95));

            final double initialDownwardSpeed = Math.max(0.0, -carrier.getVelocity().getY());
            if (initialDownwardSpeed > 0) {
                Vector vel = carrier.getVelocity();
                vel.setY(-initialDownwardSpeed * 0.6);
                carrier.setVelocity(vel);
            }

            final long startTick = Bukkit.getCurrentTick();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!gliding || !carrier.isValid() || carrier.isGliding() || carrier.isOnGround()) {
                        carrier.getWorld().getNearbyPlayers(carrier.getLocation(), 25, 25, 25)
                                .forEach(nearby -> nearby.stopSound(Sound.ITEM_ELYTRA_FLYING));
                        cancel();
                        return;
                    }

                    if (carrier.getFallDistance() >= 4) carrier.setFallDistance(carrier.getFallDistance() * 0.8f);

                    long ticks = Bukkit.getCurrentTick() - startTick;

                    // decay curve: starts stronger and asymptotically approaches configured minDownward
                    double k = soulType().decayK.cached();
                    double decayFactor = soulType().minDownward.cached() + (1 - soulType().minDownward.cached()) * Math.exp(-k * ticks);

                    double desiredDownward = Math.max(initialDownwardSpeed * decayFactor, soulType().minDownward.cached());
                    double desiredY = -desiredDownward;

                    Vector currentVelocity = carrier.getVelocity();
                    double smoothFactor = soulType().smooth.cached();
                    double newY = currentVelocity.getY() * (1 - smoothFactor) + desiredY * smoothFactor;

                    if (newY > 0) newY = Math.min(newY, 0);

                    currentVelocity.setY(newY);
                    currentVelocity.add(carrier.getLocation().getDirection().normalize().setY(0).multiply(soulType().forwardMultiplier.cached()));
                    carrier.setVelocity(currentVelocity);

                    if (ticks % 5 == 0) {
                        carrier.getWorld().spawnParticle(Particle.END_ROD, carrier.getLocation(), 5, 0.1, 0, 0.1, 0.01);
                        carrier.getWorld().playSound(carrier.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.25f, 0.75f);
                    }

                }
            }.runTaskTimer(SoulSnatcher.getPlugin(), 1L, 1L);
        }

        @Override
        public void onSneakToggle(Player carrier, PlayerToggleSneakEvent event) {
            if (carrier.isFlying() || carrier.isGliding())
                return;

            gliding = event.isSneaking();

            if (gliding)
                activateGliding();
        }

        @Override
        protected void cleanUp() {
            gliding = false;
        }
    }
}
