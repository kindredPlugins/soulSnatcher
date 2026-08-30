package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.AttributeSoul;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class DrownedSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<DrownedSoulType> create(LivingEntity carrier) {
        return new DrownedSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "drowned_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.DROWNED;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.ATTRIBUTES;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "c84df79c49104b198cdad6d99fd0d0bcf1531c92d4ab6269e40b7d3cbbb8e98c";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Drowned Soul", TextColor.color(0x4d9280));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Boosts water movement and oxygen by default."),
                Component.text("When entering water, enter a ")
                        .append(Component.text("Water State", TextColor.color(0x5e6e92))),
                Component.text("which grants additional movement speed and adds "),
                Component.text("+" + waterDamageBonus.cached(), NamedTextColor.GOLD)
                        .append(Component.text(" to melee and trident attacks.", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String ENTER_WATER_TICKS_CONFIG_ID = "enter_water_ticks";
    private static final String EXIT_WATER_TICKS_CONFIG_ID = "exit_water_ticks";
    private static final String WATER_MOVEMENT_BOOST_CONFIG_ID = "water_movement_boost";
    private static final String WATER_DAMAGE_BONUS_CONFIG_ID = "water_damage_bonus";
    private static final String OXYGEN_BONUS_CONFIG_ID = "oxygen_bonus";
    private static final String WATER_EFFICIENCY_CONFIG_ID = "water_efficiency";

    private final ConfigOption<Integer> enterWaterTicks = configOption(ENTER_WATER_TICKS_CONFIG_ID, 20, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Integer> exitWaterTicks = configOption(EXIT_WATER_TICKS_CONFIG_ID, 40, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Double> waterMovementBoost = configOption(WATER_MOVEMENT_BOOST_CONFIG_ID, 0.15, FileConfiguration::getDouble);
    private final ConfigOption<Double> waterDamageBonus = configOption(WATER_DAMAGE_BONUS_CONFIG_ID, 3.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> oxygenBonus = configOption(OXYGEN_BONUS_CONFIG_ID, 6.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> waterEfficiency = configOption(WATER_EFFICIENCY_CONFIG_ID, 1.0, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                ENTER_WATER_TICKS_CONFIG_ID, "Amount of ticks until the water state kicks in in (20 ticks =  1 second)",
                EXIT_WATER_TICKS_CONFIG_ID, "How long the water state persists when leaving water in ticks (20 ticks = 1 second)",
                WATER_MOVEMENT_BOOST_CONFIG_ID, "Movement Speed additional multiplier while in water state, applied as total multiplier (0.15 => +15%)",
                WATER_DAMAGE_BONUS_CONFIG_ID, "Flat damage added on hit while in water state",
                OXYGEN_BONUS_CONFIG_ID, "Static oxygen bonus, works like Respiration levels (1 level = +15 second oxygen)",
                WATER_EFFICIENCY_CONFIG_ID, "Static water movement efficiency, 1 means 100% => no water slowdown, identical and not stackable with depth strider"
        );
    }

    //endregion

    public static class DrownedSoulInstance extends AttributeSoul<DrownedSoulType> implements OnDamageDealtTrigger {

        private static final int TASK_INTERVAL = 2;

        private ScheduledTask waterTask;
        private boolean activeWaterBonus;

        private int enterWaterTicks;
        private int remainingWaterTicks;

        protected DrownedSoulInstance(LivingEntity carrier, DrownedSoulType soulType) {
            super(carrier, soulType);

            waterTask = carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), _ -> {
                if (activeWaterBonus) {
                    carrier.getWorld().spawnParticle(Particle.BUBBLE_POP, carrier.getEyeLocation().clone().add(0, -0.6, 0),
                            2, 0.25, 0.5, 0.5, 0);

                    if (!carrier.isInWater() && !carrier.isInRain()) {
                        remainingWaterTicks--;

                        if (remainingWaterTicks > 0)
                            return;

                        remainingWaterTicks = 0;
                        activeWaterBonus = false;

                        carrier.getWorld().playSound(carrier.getLocation(), Sound.BLOCK_SPONGE_ABSORB, 1f, 1.5f);
                        carrier.getWorld().spawnParticle(Particle.DRIPPING_WATER, carrier.getLocation().clone().add(0, carrier.getEyeHeight() / 2, 0),
                                30, 0.3, 0.5, 0.3);

                        var movSpeed = carrier.getAttribute(Attribute.MOVEMENT_SPEED);
                        if (movSpeed != null)
                            movSpeed.removeModifier(attributeKey);
                    } else {
                        remainingWaterTicks = soulType.exitWaterTicks.cached() / TASK_INTERVAL;
                    }
                } else {
                    if (carrier.isInWater() || carrier.isInRain()) {
                        enterWaterTicks++;

                        if (enterWaterTicks < soulType.enterWaterTicks.cached() / TASK_INTERVAL)
                            return;

                        enterWaterTicks = 0;
                        activeWaterBonus = true;

                        carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, 1f, 1f);

                        var movSpeed = carrier.getAttribute(Attribute.MOVEMENT_SPEED);
                        if (movSpeed != null)
                            movSpeed.addModifier(createModifier(soulType.waterMovementBoost.cached(), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
                    } else {
                        enterWaterTicks = 0;
                    }
                }
            }, () -> waterTask = null, TASK_INTERVAL, TASK_INTERVAL);
        }

        @Override
        public Map<Attribute, AttributeModifier> attributeModifierMap() {
            return Map.of(
                    Attribute.WATER_MOVEMENT_EFFICIENCY, createModifier(soulType().waterEfficiency.cached(), AttributeModifier.Operation.ADD_NUMBER),
                    Attribute.OXYGEN_BONUS, createModifier(soulType().oxygenBonus.cached(), AttributeModifier.Operation.ADD_NUMBER)
            );
        }

        @Override
        protected void extraCleanUp() {
            var movSpeed = carrier().getAttribute(Attribute.MOVEMENT_SPEED);
            if (movSpeed != null)
                movSpeed.removeModifier(attributeKey);

            if (waterTask != null)
                waterTask.cancel();

            waterTask = null;
            super.extraCleanUp();
        }

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if (!activeWaterBonus) return;

            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

            if (event.getDamageSource().isIndirect() && !(event.getDamager() instanceof Trident))
                return;

            event.setDamage(event.getDamage() + soulType().waterDamageBonus.cached());

            target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation().add(0, target.getEyeHeight() / 2, 0),
                    150, 0.25, 0.5, 0.25, 0);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 1.5f);
        }
    }
}
