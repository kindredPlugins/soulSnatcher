package at.gaderman.soulSnatcher.souls.instances.combat.targeting;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoService(SoulType.class)
public class SquidSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<SquidSoulType> create(LivingEntity carrier) {
        return new SquidSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "squid_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SQUID;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "464bdc6f600656511bef596c1a16aab1d3f5dbaae8bee19d5c04de0db21ce92c";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Squid Soul", TextColor.color(0x1b3243));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When damaged applies ")
                        .append(Component.text("ink ", TextColor.color(0x1E2324)))
                        .append(Component.text("onto nearby")),
                Component.text("enemies causing blindness and follow range loss")
        );
    }

    //region Config Values

    private static final String INK_COOLDOWN_CONFIG_ID = "ink_cooldown";
    private static final String INK_DURATION_CONFIG_ID = "aura_range";

    private final ConfigOption<Integer> inkCooldown = configOption(INK_COOLDOWN_CONFIG_ID, 8000, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Integer> inkDuration = configOption(INK_DURATION_CONFIG_ID, 60, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                INK_COOLDOWN_CONFIG_ID, "Cooldown between each damage taken triggers the ink effect in milliseconds (1000ms = 1s)",
                INK_DURATION_CONFIG_ID, "How long the ink effect persists in ticks (20 ticks = 1 second)"
        );
    }

    //endregion

    public static class SquidSoulInstance extends TargetTrackerSoulInstance<SquidSoulType> {
        protected SquidSoulInstance(LivingEntity carrier, SquidSoulType soulType) {
            super(carrier, soulType);
        }

        private long lastInkBurst;

        private static final NamespacedKey INK_DEBUFF = new NamespacedKey(SoulSnatcher.getPlugin(), "ink_debuff");

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            super.onDamageReceivedByEntity(carrier, damager, event);

            if (lastInkBurst > System.currentTimeMillis() - soulType().inkCooldown.cached()) return;

            lastInkBurst = System.currentTimeMillis();

            combatTargets = combatTargets.stream().filter(Entity::isValid).collect(Collectors.toSet());

            carrier.getWorld().spawnParticle(Particle.SQUID_INK, carrier.getLocation().add(0, 1, 0), 50,
                    0, 0, 0, 0.1);
            carrier.getWorld().spawnParticle(Particle.LARGE_SMOKE, carrier.getLocation().add(0, 1, 0), 10,
                    0, 0, 0, 0.1);
            carrier.getWorld().playSound(carrier, Sound.ENTITY_SQUID_HURT, 1f, 1f);

            carrier.getWorld().getNearbyLivingEntities(carrier.getLocation(), 5).forEach(target -> {
                if (!combatTargets.contains(target)) return;

                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, soulType().inkDuration.cached(), 0, true));

                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (!target.isValid() || ticks >= soulType().inkDuration.cached()) {
                            cancel();
                            return;
                        }

                        target.getWorld().spawnParticle(Particle.SQUID_INK, target.getEyeLocation(), 1,
                                0.2, 0.2, 0.2, 0);

                        ticks += 2;
                    }
                }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, 2L);

                if (target instanceof Mob mob) {
                    var followRange = mob.getAttribute(Attribute.FOLLOW_RANGE);
                    if (followRange != null)
                        followRange.addModifier(new AttributeModifier(INK_DEBUFF, -0.85, AttributeModifier.Operation.ADD_SCALAR));

                    var movSpeed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
                    if (movSpeed != null)
                        movSpeed.addModifier(new AttributeModifier(INK_DEBUFF, -0.2, AttributeModifier.Operation.ADD_SCALAR));

                    mob.getPersistentDataContainer().set(INK_DEBUFF, PersistentDataType.STRING, carrier.getUniqueId().toString());

                    SoulSnatcher.getPlugin().registerDelayedTask(() -> {
                        if (mob.isDead()) return;

                        if (!mob.getPersistentDataContainer().getOrDefault(INK_DEBUFF, PersistentDataType.STRING, "")
                                .equals(carrier.getUniqueId().toString())) return;

                        if (followRange != null) followRange.removeModifier(INK_DEBUFF);
                        if (movSpeed != null) movSpeed.removeModifier(INK_DEBUFF);

                        mob.getPersistentDataContainer().remove(INK_DEBUFF);
                    }, soulType().inkDuration.cached());
                }
            });
        }
    }
}
