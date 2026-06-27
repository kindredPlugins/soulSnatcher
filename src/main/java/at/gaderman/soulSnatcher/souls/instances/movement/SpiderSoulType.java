package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.mobGoals.ability.SpiderDashGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnPlayerJumpTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class SpiderSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<SpiderSoulType> create(LivingEntity carrier) {
        return new SpiderSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "spider_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SPIDER;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "35e248da2e108f09813a6b848a0fcef111300978180eda41d3d1a7a8e4dba3c3";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Spider Soul", TextColor.color(0x9c0000));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.keybind("key.sneak", NamedTextColor.GOLD)
                        .append(Component.text(" + Jump "))
                        .append(Component.text("to spider dash.", NamedTextColor.WHITE)),
                Component.text("Will leap you forward a large"),
                Component.text("horizontal distance ", NamedTextColor.AQUA)
                        .append(Component.text("on a short cooldown.", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String JUMP_COOLDOWN_CONFIG_ID = "jump_cooldown";
    private static final String DASH_POWER_CONFIG_ID = "dash_power";
    private static final String DASH_VERTICAL_CONFIG_ID = "dash_vertical";
    private static final String DASH_EXHAUSTION_CONFIG_ID = "dash_exhaustion";

    private final ConfigOption<Integer> jumpCooldown = configOption(JUMP_COOLDOWN_CONFIG_ID, 1000, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Double> dashPower = configOption(DASH_POWER_CONFIG_ID, 0.75, FileConfiguration::getDouble);
    private final ConfigOption<Double> dashVertical = configOption(DASH_VERTICAL_CONFIG_ID, 0.2, FileConfiguration::getDouble);
    private final ConfigOption<Double> dashExhaustion = configOption(DASH_EXHAUSTION_CONFIG_ID, 2.75, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                JUMP_COOLDOWN_CONFIG_ID, "Cooldown for being able to spider jump in milliseconds (1000ms = 1s)",
                DASH_POWER_CONFIG_ID, "How much horizontal force the dash applies (using direction vector multiplication)",
                DASH_EXHAUSTION_CONFIG_ID, "How much exhaustion does it cost to do the spider jump (2.75 is about one hunger point, 0 would remove this)"
        );
    }

    //endregion

    public static class SpiderSoulInstance extends SoulInstance<SpiderSoulType> implements OnPlayerJumpTrigger {
        protected SpiderSoulInstance(LivingEntity carrier, SpiderSoulType soulType) {
            super(carrier, soulType);

            if(carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new SpiderDashGoal(mob));
        }

        //TODO: movement mechanics are op in minecraft might look into this later but requires some testing from actual players and gameplay to see

        private long lastJump;
        // replaced with config option

        @Override
        public void onPlayerJump(Player carrier, PlayerJumpEvent event) {
            if(!carrier.isSneaking() || lastJump > System.currentTimeMillis() - soulType().jumpCooldown.cached()) return;

            lastJump = System.currentTimeMillis();

            carrier.setVelocity(carrier.getVelocity().add(carrier.getLocation().getDirection().multiply(soulType().dashPower.cached()).setY(soulType().dashVertical.cached())));
            carrier.setExhaustion(carrier.getExhaustion() + soulType().dashExhaustion.cached().floatValue());

            carrier.getWorld().spawnParticle(Particle.SWEEP_ATTACK, carrier.getLocation().add(0, 0.2, 0), 1, 0, 0, 0, 0);
            carrier.getWorld().playSound(carrier, Sound.ENTITY_SPIDER_AMBIENT, 1f, 1.5f);

        }
    }
}
