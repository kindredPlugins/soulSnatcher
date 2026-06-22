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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class ZombifiedPiglinSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<ZombifiedPiglinSoulType> create(LivingEntity carrier) {
        return new ZombifiedPiglinSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "zombified_piglin_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.ZOMBIFIED_PIGLIN;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "e935842af769380f78e8b8a88d1ea6ca2807c1e5693c2cf797456620833e936f";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Zombified Piglin Soul", TextColor.color(0xf19e98));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When engaging in combat ")
                        .append(Component.text("mark ", NamedTextColor.DARK_RED)),
                Component.text("a target and gain ")
                        .append(Component.text("+ " + engageBoost.cached() * 100 + "% Movement Speed", NamedTextColor.AQUA)),
                Component.text("Mark automatically jumps to the next target on death", NamedTextColor.GRAY)
        );
    }

    //region Config Values

    private static final String ENGAGE_TIMEOUT_CONFIG_ID = "engage_timeout";
    private static final String ENGAGE_BOOST_CONFIG_ID = "engage_boost";

    private final ConfigOption<Integer> engageTimeout = configOption(ENGAGE_TIMEOUT_CONFIG_ID, 2000, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Double> engageBoost = configOption(ENGAGE_BOOST_CONFIG_ID, 0.2, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                ENGAGE_TIMEOUT_CONFIG_ID, "Time how long after not actively engaging in combat for the mark to run out in milliseconds (1000ms = 1s)",
                ENGAGE_BOOST_CONFIG_ID, "Factor by how much the carriers walk speed increases while being engaged with a mark (applied as %, 0.2 -> 120%)"
        );
    }

    //endregion

    public static class ZombifiedPiglinSoulInstance extends TargetTrackerSoulInstance<ZombifiedPiglinSoulType> {
        protected ZombifiedPiglinSoulInstance(LivingEntity carrier, ZombifiedPiglinSoulType soulType) {
            super(carrier, soulType);
        }

        private BlockDisplay markDisplay;
        private LivingEntity marked;

        private long lastMarkEngage;

        private static final NamespacedKey HUNT_BOOST = new NamespacedKey(SoulSnatcher.getPlugin(), "zombified_piglin_hunter");

        //TODO: potential bottle neck has been established, combatTargets seem to not be cleared when entities are invalid, need some sort of garbage collection either timeout or on add/remove events
        @Override
        protected void addCombatTarget(LivingEntity target) {
            super.addCombatTarget(target);

            if (marked == null) {
                if (carrier().getAttribute(Attribute.MOVEMENT_SPEED).getModifier(HUNT_BOOST) == null)
                    carrier().getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier(HUNT_BOOST, soulType().engageBoost.cached(), AttributeModifier.Operation.ADD_SCALAR));

                selectNewMark(target);

                this.markDisplay = target.getWorld().spawn(target.getLocation(), BlockDisplay.class, markDisplay -> {
                    markDisplay.setVisibleByDefault(false);
                    if (carrier() instanceof Player player)
                        player.showEntity(SoulSnatcher.getPlugin(), markDisplay);

                    markDisplay.setBlock(Material.TARGET.createBlockData());
                    markDisplay.setGlowColorOverride(Color.RED);
                    markDisplay.setGlowing(true);

                    Transformation transformation = markDisplay.getTransformation();
                    float scale = 0.35f;
                    transformation.getScale().set(scale, scale, scale);
                    transformation.getTranslation().set(-scale / 2, 0, -scale / 2);
                    markDisplay.setTransformation(transformation);

                    markDisplay.setBillboard(Display.Billboard.FIXED);
                    markDisplay.setPersistent(false);

                    markDisplay.setInterpolationDelay(0);
                    markDisplay.setInterpolationDuration(2);
                });

                new BukkitRunnable() {
                    final LivingEntity carrier = carrier();

                    @Override
                    public void run() {
                        if (!carrier.isValid()) {
                            resetMark();
                            cancel();
                            return;
                        }

                        if (!marked.isValid() || lastMarkEngage < System.currentTimeMillis() - soulType().engageTimeout.cached()) {
                            combatTargets.remove(marked);

                            LivingEntity nextMark = combatTargets.stream()
                                    .filter(target -> target.isValid() && carrier.hasLineOfSight(target))
                                    .findFirst().orElse(null);

                            if (nextMark == null) {
                                resetMark();
                                cancel();
                                return;
                            }

                            selectNewMark(nextMark);
                        }

                        Vector direction = marked.getLocation().getDirection().setY(0);
                        markDisplay.teleport(marked.getLocation()
                                .setDirection(direction)
                                .add(0, marked.getEyeHeight() + 0.5, 0));

                        carrier.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, carrier.getEyeLocation().add(0, 0.25, 0),
                                1, 0.2, 0.1, 0.2, 0.3);
                    }
                }.runTaskTimer(SoulSnatcher.getPlugin(), 2L, 2L);
            }
        }

        private void selectNewMark(LivingEntity mark) {
            this.marked = mark;
            lastMarkEngage = System.currentTimeMillis();
            mark.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));

            LivingEntity carrier = carrier();
            carrier.getWorld().playSound(carrier, Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1f, 1f);

            Location targetLoc = mark.getEyeLocation();
            Location origin = carrier.getEyeLocation();
            @NotNull Vector markDirection = targetLoc.toVector().subtract(origin.toVector()).normalize();
            double markY = markDirection.getY();
            markDirection = markDirection.multiply(0.5).setY(markY);

            int i = 0;
            while (origin.distance(targetLoc) > 0.5 && i < 100) {
                origin.add(markDirection);

                origin.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, origin, 1, 0, 0, 0,
                        0, Material.REDSTONE_BLOCK.createBlockData());

                i++;
            }
        }

        private void resetMark() {
            marked = null;
            combatTargets.clear();
            markDisplay.remove();
            carrier().getWorld().playSound(carrier(), Sound.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT, 0.8f, 0.5f);
            carrier().getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(HUNT_BOOST);
        }

        @Override
        protected void cleanUp() {
            resetMark();
        }
    }
}
