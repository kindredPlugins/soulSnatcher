package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
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
public class StriderSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<StriderSoulType> create(LivingEntity carrier) {
        return new StriderSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "strider_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.STRIDER;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "65ccbb547820b667cc9d3bc9fff1e3d65da2375655a3427b30e1d009eeb272ce";
    }

    @Override
    public @NotNull Component defaultDisplayName() {
        return Component.text("Strider", TextColor.color(0xa33538));
    }

    @Override
    public @NotNull List<Component> defaultDescription() {
        return List.of(
            Component.text("Gain the ability to walk and jump on lava."),
                Component.text("(Comes with lava resistance)", NamedTextColor.GRAY)
        );
    }

    //region Config Values

    private static final String FORWARD_PULL_CONFIG_ID = "forward_pull";
    private final ConfigOption<Double> forwardPull = configOption(FORWARD_PULL_CONFIG_ID, 0.1, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(FORWARD_PULL_CONFIG_ID, "Vector multiplication to the players forward direction when pressing forward while walking on lava. Applied per tick touching lava. (0.1 -> fast strider speed)");
    }

    //endregion

    public static class StriderSoulInstance extends SoulInstance<StriderSoulType> implements OnDamageReceivedTrigger {

        protected StriderSoulInstance(LivingEntity carrier, StriderSoulType soulType) {
            super(carrier, soulType);

            carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), _ -> {
                if(carrier.isSneaking())
                    return;

                Location location = carrier.getLocation();

                if(location.getBlock().getType() != Material.LAVA)
                    return;

                carrier.setVelocity(carrier.getVelocity().setY(0.1));

                if(carrier instanceof Player player){
                    Input input = player.getCurrentInput();

                    if(input.isForward())
                        player.setVelocity(player.getVelocity().add(player.getLocation().getDirection().multiply(0.1)));

                    if(input.isJump())
                        player.setVelocity(player.getVelocity().add(new Vector(0, 0.35, 0)));
                }

                Particle.LARGE_SMOKE.builder()
                        .location(carrier.getLocation())
                        .offset(0.4, 0.2, 0.4)
                        .count(2)
                        .speed(0)
                        .spawn();

                if(Bukkit.getCurrentTick() % 5 == 0)
                    carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_STRIDER_STEP_LAVA, 1f, 1f);
            }, null, 1, 1);
        }


        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if(event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                event.setCancelled(true);
                carrier.setFireTicks(0);
            }
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
        }
    }
}
