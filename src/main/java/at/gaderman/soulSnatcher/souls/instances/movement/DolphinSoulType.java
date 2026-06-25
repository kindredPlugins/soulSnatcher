package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.mobGoals.targeting.WaterMovementGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSwimToggleTrigger;
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
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class DolphinSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<DolphinSoulType> create(LivingEntity carrier) {
        return new DolphinSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "dolphin_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.DOLPHIN;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "8e9688b950d880b55b7aa2cfcd76e5a0fa94aac6d16f78e833f7443ea29fed3";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Dolphin Soul", TextColor.color(0xbdcbdd));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("While swimming gain ")
                        .append(Component.text("Dolphins Grace", NamedTextColor.AQUA)),
                Component.text("Swimming to the water surface makes you"),
                Component.text("dolphin dash ahead")
        );
    }

    //region Config Values

    private static final String JUMP_MULTIPLIER_CONFIG_ID = "dolphin_jump_multiplier";
    private static final String JUMP_Y_CONFIG_ID = "dolphin_jump_y";

    private final ConfigOption<Double> jumpMultiplier = configOption(JUMP_MULTIPLIER_CONFIG_ID, 1.5, FileConfiguration::getDouble);
    private final ConfigOption<Double> jumpY = configOption(JUMP_Y_CONFIG_ID, 1.0, FileConfiguration::getDouble);

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                JUMP_MULTIPLIER_CONFIG_ID, "The horizontal force when dolphin jumping out of water (applied as direction vector multiplication)",
                JUMP_Y_CONFIG_ID, "Fixed y value of the dolphin jump vector"
        );
    }

    //endregion

    public static class DolphinSoulInstance extends SoulInstance<DolphinSoulType> implements OnSwimToggleTrigger {
        protected DolphinSoulInstance(LivingEntity carrier, DolphinSoulType soulType) {
            super(carrier, soulType);

            if(carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new WaterMovementGoal(mob));
        }

        @Override
        public void onSwimToggle(LivingEntity carrier, EntityToggleSwimEvent event) {
            if(!(carrier instanceof Player)) return;

            if(event.isSwimming()) {
                carrier.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, PotionEffect.INFINITE_DURATION, 1));
                carrier.getWorld().playSound(carrier, Sound.ENTITY_DOLPHIN_PLAY, 1f, 1.2f);
                carrier.getWorld().spawnParticle(Particle.BUBBLE, carrier.getLocation(), 100);
            }else {
                if(!carrier.isUnderWater()){
                    doDolphinJump(carrier, carrier.getLocation().getDirection(), soulType().jumpMultiplier.cached(), soulType().jumpY.cached());
                }

                carrier.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
            }
        }

        public static void doDolphinJump(LivingEntity entity, Vector jumpDirection, double multiplier, double y){
            entity.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, entity.getLocation(), 100, 0.5, 0, 0.5, 0.2);
            entity.getWorld().spawnParticle(Particle.SPLASH, entity.getLocation(), 100, 0.5, 0.5, 0.5, 1);
            entity.getWorld().playSound(entity.getLocation(), Sound.AMBIENT_UNDERWATER_EXIT, 2f, 2f);

            entity.setVelocity(jumpDirection.normalize().multiply(multiplier).setY(y));
        }
    }
}
