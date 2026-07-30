package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class RavagerSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<RavagerSoulType> create(LivingEntity carrier) {
        return new RavagerSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "ravager_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.RAVAGER;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "cd20bf52ec390a0799299184fc678bf84cf732bb1bd78fd1c4b441858f0235a8";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Ravager Soul", TextColor.color(0xe5b5049));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When taking melee damage unleash a ")
                        .append(Component.text("roar", NamedTextColor.GOLD)),
                Component.text("which knocks nearby enemies away.")
        );
    }

    //region Config Values

    private static final String SHOUT_COOLDOWN_CONFIG_ID = "shout_cooldown";
    private static final String SHOUT_STRENGTH_CONFIG_ID = "shout_strength";
    private static final String SHOUT_RADIUS_CONFIG_ID = "shout_radius";

    private final ConfigOption<Integer> shoutCooldown = configOption(SHOUT_COOLDOWN_CONFIG_ID, 6000, FileConfiguration::getInt, value -> Math.clamp(value, 0, 1));
    private final ConfigOption<Double> shoutStrength = configOption(SHOUT_STRENGTH_CONFIG_ID, 2.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> shoutRadius = configOption(SHOUT_RADIUS_CONFIG_ID, 4.0, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                SHOUT_COOLDOWN_CONFIG_ID, "Cooldown between shout attacks in milliseconds (1000ms = 1s)",
                SHOUT_STRENGTH_CONFIG_ID, "Strength of the shout attack as power for horizontal push force",
                SHOUT_RADIUS_CONFIG_ID, "Radius of the shout attack"
        );
    }

    //endregion

    public static class RavagerSoulInstance extends SoulInstance<RavagerSoulType> implements OnDamageReceivedTrigger {
        protected RavagerSoulInstance(LivingEntity carrier, RavagerSoulType soulType) {
            super(carrier, soulType);
        }

        private long lastSnout;

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            if (event.getDamageSource().isIndirect())
                return;

            if (lastSnout > System.currentTimeMillis() - soulType().shoutCooldown.cached())
                return;

            lastSnout = System.currentTimeMillis();
            carrier.getWorld().playSound(carrier, Sound.ENTITY_RAVAGER_STUNNED, 1f, 1f);

            carrier.getScheduler().execute(SoulSnatcher.getPlugin(), () -> {
                carrier.getWorld().playSound(carrier, Sound.ENTITY_RAVAGER_ROAR, 1f, 1f);
                carrier.getWorld().spawnParticle(Particle.WHITE_SMOKE, carrier.getEyeLocation(), 100, 0.5, 0.5, 0.5, 0.05);

                double radius = soulType().shoutRadius.cached();
                carrier.getNearbyEntities(radius, radius, radius).forEach(entity -> {
                    Vector direction = entity.getLocation().subtract(carrier.getLocation()).toVector().normalize().setY(0.3);
                    double distanceFactor = Math.clamp(entity.getLocation().distance(carrier.getLocation()) / radius, 0.75, 1);
                    double resistance = 0;

                    if (entity instanceof LivingEntity livingEntity) {
                        var knockRes = livingEntity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                        if (knockRes != null) {
                            resistance = Math.clamp(knockRes.getValue(), 0, 1);
                        }
                    }

                    entity.setVelocity(direction.multiply(soulType().shoutStrength.cached()).multiply(distanceFactor).multiply(1 - resistance));
                });
            }, null, 15L);

            if (carrier instanceof Player player) {
                player.getScheduler().execute(SoulSnatcher.getPlugin(), () -> {
                    player.playSound(player, Sound.ENTITY_RAVAGER_AMBIENT, 1f, 1.5f);
                }, null, soulType().shoutCooldown.cached() / 50);
            }
        }
    }
}
