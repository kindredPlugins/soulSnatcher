package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.instances.combat.targeting.TargetTrackerSoulInstance;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class EvokerSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<EvokerSoulType> create(LivingEntity carrier) {
        return new EvokerSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "evoker_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.EVOKER;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "3433322e2ccbd9c55ef41d96f38dbc666c803045b24391ac9391dccad7cd";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Evoker Soul", TextColor.color(0xc7c06b));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When dealing damage will summon a line of"),
                Component.text("evoker fangs ", NamedTextColor.GREEN)
                        .append(Component.text("every ", NamedTextColor.WHITE))
                        .append(Component.text(fangsCooldown.cached() / 1000.0 + "s", NamedTextColor.AQUA))
                        .append(Component.text(".", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String SUMMON_COOLDOWN_CONFIG_ID = "fangs_cooldown";
    private static final String FANGS_RANGE_MULTIPLIER_CONFIG_ID = "fangs_range_multiplier";

    private final ConfigOption<Integer> fangsCooldown = configOption(SUMMON_COOLDOWN_CONFIG_ID, 2500, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Double> fangsRangeMultiplier = configOption(FANGS_RANGE_MULTIPLIER_CONFIG_ID, 2.0, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                SUMMON_COOLDOWN_CONFIG_ID, "Cooldown between fangs appearing after an attack in milliseconds (1000 ms = 1 s)",
                FANGS_RANGE_MULTIPLIER_CONFIG_ID, "Multiplier on how far fangs travel based on distance to target (2.0 -> 2x the distance towards the target)"
        );
    }

    //endregion

    public static class EvokerSoulInstance extends TargetTrackerSoulInstance<EvokerSoulType> implements OnDamageDealtTrigger {
        protected EvokerSoulInstance(LivingEntity carrier, EvokerSoulType soulType) {
            super(carrier, soulType);
        }

        private long lastFangAttack;

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if(event.getDamager() instanceof EvokerFangs) {
                if(!combatTargets.contains(target))
                    event.setCancelled(true);

                return;
            }

            if(lastFangAttack > System.currentTimeMillis() - soulType().fangsCooldown.cached())
                return;

            lastFangAttack = System.currentTimeMillis();

            Vector direction = target.getLocation().subtract(carrier.getLocation()).toVector().normalize().setY(0);
            double distance = Math.ceil(target.getLocation().distance(carrier.getLocation()));
            Location origin = carrier.getLocation();

            for (double i = 0; i < distance * soulType().fangsRangeMultiplier.cached(); i++) {
                Location location = origin.clone().add(direction.clone().multiply(i));

                int yOffset = 0;
                while (yOffset < 5 && !isValidFangLocation(location)) {
                    location.add(0, -1, 0);
                    yOffset++;
                }

                if (!isValidFangLocation(location)) {
                    location = origin.clone().add(direction.clone().multiply(i));

                    yOffset = 0;
                    while (yOffset < 10 && !isValidFangLocation(location)) {
                        location.add(0, 1, 0);
                        yOffset++;
                    }
                }

                if (isValidFangLocation(location)) {
                    double finalI = i;
                    location.getWorld().spawn(location, EvokerFangs.class, fangs -> {
                        fangs.setOwner(carrier);
                        fangs.setAttackDelay((int) finalI);
                    });
                }
            }

            carrier.getWorld().playSound(carrier, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f);
        }

        private boolean isValidFangLocation(Location loc) {
            return loc.getBlock().isPassable()
                    && !loc.getBlock().getRelative(BlockFace.DOWN).isPassable();
        }
    }
}
