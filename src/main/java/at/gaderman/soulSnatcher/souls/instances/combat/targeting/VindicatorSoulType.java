package at.gaderman.soulSnatcher.souls.instances.combat.targeting;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSprintToggleTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@AutoService(SoulType.class)
public class VindicatorSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<VindicatorSoulType> create(LivingEntity carrier) {
        return new VindicatorSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "vindicator_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.VINDICATOR;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "9e1cab382458e843ac4356e3e00e1d35c36f449fa1a84488ab2c6557b392d";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Vindicator Soul", TextColor.color(0x959b9b));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When ")
                        .append(Component.text("sprinting ", NamedTextColor.GREEN))
                        .append(Component.text("while in combat ", NamedTextColor.WHITE)),
                Component.text("gain ")
                        .append(Component.text("+" + moveBonus.cached() * 100 + "% Movement Speed", NamedTextColor.AQUA)),
                Component.text("and ")
                        .append(Component.text("+" + atspBonus.cached() * 100 + "% Attack Speed", NamedTextColor.AQUA))
        );
    }

    //region Config Values

    private static final String MOVE_BONUS_CONFIG_ID = "movement_bonus";
    private static final String ATSP_BONUS_CONFIG_ID = "attack_speed_bonus";

    private final ConfigOption<Double> moveBonus = configOption(MOVE_BONUS_CONFIG_ID, 0.1, FileConfiguration::getDouble);
    private final ConfigOption<Double> atspBonus = configOption(ATSP_BONUS_CONFIG_ID, 0.1, FileConfiguration::getDouble);

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                MOVE_BONUS_CONFIG_ID, "How much walk speed increases when sprinting in combat (in %, 0.1 -> 110%)",
                ATSP_BONUS_CONFIG_ID, "How much attack speed increases when sprinting in combat (in %, 0.1 -> 110%)"
        );
    }

    //endregion

    public static class VindicatorSoulInstance extends TargetTrackerSoulInstance<VindicatorSoulType> implements OnSprintToggleTrigger {
        protected VindicatorSoulInstance(LivingEntity carrier, VindicatorSoulType soulType) {
            super(carrier, soulType);

            resetMods();
        }

        private static final NamespacedKey BOOST = new NamespacedKey(SoulSnatcher.getPlugin(), "vindicator_boost");

        private BukkitTask checkCombatStateTask;

        @Override
        public void onSprintToggle(Player carrier, PlayerToggleSprintEvent event) {
            combatTargets = combatTargets.stream()
                    .filter(target -> target.isValid() && carrier.hasLineOfSight(target))
                    .collect(Collectors.toSet());

            if(!event.isSprinting()) {
                resetMods();
                return;
            }

            if(combatTargets.isEmpty()) return;

            applyBoost(carrier);
        }

        private void applyBoost(LivingEntity carrier) {
            var movSpeed = carrier.getAttribute(Attribute.MOVEMENT_SPEED);
            if (movSpeed != null)
                movSpeed.addModifier(new AttributeModifier(BOOST, soulType().moveBonus.cached(), AttributeModifier.Operation.MULTIPLY_SCALAR_1));

            var atkSpeed = carrier.getAttribute(Attribute.ATTACK_SPEED);
            if (atkSpeed != null)
                atkSpeed.addModifier(new AttributeModifier(BOOST, soulType().atspBonus.cached(), AttributeModifier.Operation.MULTIPLY_SCALAR_1));

            carrier.getWorld().playSound(carrier, Sound.ENTITY_VINDICATOR_AMBIENT, 1f, 1.5f);
            carrier.getWorld().spawn(carrier.getLocation().clone().add(0, 2, 0).setDirection(carrier.getLocation().getDirection().setY(0)), ItemDisplay.class, display -> {
               display.setItemStack(ItemStack.of(Material.EMERALD));
               display.setBillboard(Display.Billboard.FIXED);
               display.setPersistent(false);

                display.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), new Consumer<>() {
                    int duration = 20;
                    final int totalTicks = 20;
                    final int spins = 3;

                    @Override
                    public void accept(ScheduledTask task) {
                        int ticksElapsed = totalTicks - duration;
                        float angle = (float) (ticksElapsed * ((360.0 * spins) / totalTicks));
                        float radians = (float) Math.toRadians(angle);

                        Transformation t = display.getTransformation();
                        t.getLeftRotation().identity().rotateY(radians);
                        display.setTransformation(t);

                        duration--;
                        if (duration <= 0) {
                            display.remove();
                            task.cancel();
                        }
                    }
                }, null, 1L, 1L);
            });

            if (checkCombatStateTask == null) {
                checkCombatStateTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!combatTargets.isEmpty()) return;

                        if (carrier.isValid() && carrier instanceof Player player)
                            player.playSound(carrier, Sound.ENTITY_VINDICATOR_HURT, 1f, 0.5f);

                        resetMods();
                        cancel();
                        checkCombatStateTask = null;
                    }
                }.runTaskTimer(SoulSnatcher.getPlugin(), 1L, 1L);
            }
        }

        @Override
        protected void addCombatTarget(LivingEntity target) {
            super.addCombatTarget(target);

            if(carrier() instanceof Mob mob && checkCombatStateTask == null)
                applyBoost(mob);
        }

        private void resetMods(){
            LivingEntity carrier = carrier();
            if(!carrier.isValid()) return;

            var movSpeed = carrier().getAttribute(Attribute.MOVEMENT_SPEED);
            if(movSpeed != null)
                movSpeed.removeModifier(BOOST);

            var atkSpeed = carrier().getAttribute(Attribute.ATTACK_SPEED);
            if(atkSpeed != null)
                atkSpeed.removeModifier(BOOST);
        }

        @Override
        protected void cleanUp() {
            super.cleanUp();

            resetMods();
        }
    }
}
