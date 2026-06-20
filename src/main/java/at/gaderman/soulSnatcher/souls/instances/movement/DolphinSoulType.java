package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.mobGoals.targeting.WaterMovementGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSwimToggleTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

@AutoService(SoulType.class)
public class DolphinSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
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
        return List.of();
    }

    public static class DolphinSoulInstance extends SoulInstance implements OnSwimToggleTrigger {
        protected DolphinSoulInstance(LivingEntity carrier, SoulType soulType) {
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
                    doDolphinJump(carrier, carrier.getLocation().getDirection());
                }

                carrier.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
            }
        }

        public static void doDolphinJump(LivingEntity entity, Vector jumpDirection){
            entity.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, entity.getLocation(), 100, 0.5, 0, 0.5, 0.2);
            entity.getWorld().spawnParticle(Particle.SPLASH, entity.getLocation(), 100, 0.5, 0.5, 0.5, 1);
            entity.getWorld().playSound(entity.getLocation(), Sound.AMBIENT_UNDERWATER_EXIT, 2f, 2f);

            entity.setVelocity(jumpDirection.normalize().multiply(1.5).setY(1));
        }
    }
}
