package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSneakToggleTrigger;
import com.google.auto.service.AutoService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class CaveSpiderSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<CaveSpiderSoulType> create(LivingEntity carrier) {
        return new CaveSpiderSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "cave_spider_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.CAVE_SPIDER;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "eccc4a32d45d74e8b14ef1ffd55cd5f381a06d4999081d52eaea12e13293e209";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    public @NotNull Component defaultDisplayName() {
        return Component.text("Cave Spider", TextColor.color(0x620000));
    }

    @Override
    public @NotNull List<Component> defaultDescription() {
        return List.of(
            Component.text("When ")
                    .append(Component.text("sneaking ", NamedTextColor.GOLD))
                    .append(Component.text("your model size will")),
                Component.text("reduced to ")
        );
    }

    private static final String SHRINK_SIZE_CONFIG_ID = "shrink_size";

    private final ConfigOption<Double> shrinkSize = configOption(SHRINK_SIZE_CONFIG_ID, 0.5, FileConfiguration::getDouble, value -> Math.clamp(value, 0.25, 0.9));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                SHRINK_SIZE_CONFIG_ID, "Multiplier of the size / scale attribute to apply, 0.5 -> 50% of their base size"
        );
    }

    public static class CaveSpiderSoulInstance extends SoulInstance<CaveSpiderSoulType> implements OnSneakToggleTrigger {

        private static final NamespacedKey SHRINK_SIZE_KEY = new NamespacedKey(SoulSnatcher.getPlugin(), "cave_spider_soul_shrink");
        private static final int SHRINK_STEPS = 3;
        private static final int ANIMATION_TICKS = 3;

        private final double totalShrink;

        private int step;
        private boolean targetShrunk;
        private ScheduledTask animationTask;

        protected CaveSpiderSoulInstance(LivingEntity carrier, CaveSpiderSoulType soulType) {
            super(carrier, soulType);
            this.totalShrink = 1 - soulType.shrinkSize.cached();

            if (carrier instanceof Mob mob) {
                step = SHRINK_STEPS;
                targetShrunk = true;
                applyStep(mob);
            }
        }

        @Override
        public void onSneakToggle(Player carrier, PlayerToggleSneakEvent event) {
            targetShrunk = event.isSneaking();

            if (animationTask != null)
                return;

            animationTask = carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(),
                    task -> tickAnimation(carrier, task),
                    this::clearTask, ANIMATION_TICKS, ANIMATION_TICKS);
        }

        private void tickAnimation(LivingEntity entity, ScheduledTask task) {
            int goal = targetShrunk ? SHRINK_STEPS : 0;

            if (step != goal) {
                step += Integer.signum(goal - step);
                applyStep(entity);
            }

            if (step == goal) {
                task.cancel();
                clearTask();
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.3f,
                        targetShrunk ? 1.5f : 0.5f);
            }
        }

        private void clearTask() {
            animationTask = null;
        }

        private void applyStep(LivingEntity entity) {
            var scale = entity.getAttribute(Attribute.SCALE);
            if (scale == null)
                return;

            scale.removeModifier(SHRINK_SIZE_KEY);

            if (step > 0) {
                double mod = -(totalShrink * step / SHRINK_STEPS);
                scale.addModifier(new AttributeModifier(SHRINK_SIZE_KEY, mod, AttributeModifier.Operation.ADD_SCALAR));
            }
        }

        @Override
        protected void cleanUp() {
            super.cleanUp();

            if(animationTask != null)
                animationTask.cancel();
        }
    }
}
