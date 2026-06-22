package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.OnTargetTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class CreeperSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance<CreeperSoulType> create(LivingEntity carrier) {
        return new CreeperSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "creeper_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.CREEPER;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "1ff8f6d00d5b07387584f117c66d698c90c69cedb01a6e69dbb02771c7302d16";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Creeper Soul", TextColor.color(0x6fdb5c));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }

    public static class CreeperSoulInstance extends SoulInstance<CreeperSoulType> implements OnTargetTrigger {

        protected CreeperSoulInstance(LivingEntity carrier, CreeperSoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public void onBeingTargeted(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event) {
            var followRangeAttr = entity.getAttribute(Attribute.FOLLOW_RANGE);
            double followRange = followRangeAttr == null ? 16 : followRangeAttr.getValue();

            double distance = entity.getLocation().distance(carrier.getLocation());
            if(distance < followRange / 2) return;

            event.setCancelled(true);

            if(carrier instanceof Player player) {
                player.spawnParticle(Particle.TINTED_LEAVES, carrier.getEyeLocation().add(0, -0.2, 0), 5, 0.2, 0,
                        0.2, 0.2, Color.LIME);
                player.playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.5f, 0.5f);
            }
        }

        @Override
        public void onCarrierTarget(LivingEntity carrier, LivingEntity target, EntityTargetLivingEntityEvent event) {
            Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
                if(event.isCancelled() || !carrier.isValid() || !target.isValid()) return;

                double distance = target.getLocation().distance(carrier.getLocation());
                if (distance <= 10) return;

                carrier.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));
                carrier.getWorld().spawnParticle(Particle.TINTED_LEAVES, carrier.getEyeLocation(), 30, 0.2, 0, 0.2,
                        0.5, Color.LIME);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!carrier.isValid()) {
                            cancel();
                            return;
                        }

                        if (!target.isValid() || carrier.getLocation().distance(target.getLocation()) <= 10) {
                            carrier.removePotionEffect(PotionEffectType.INVISIBILITY);
                            carrier.getWorld().spawnParticle(Particle.TINTED_LEAVES, carrier.getEyeLocation(), 50, 0.2, 0.5, 0.2,
                                    1.5, Color.LIME);

                            cancel();
                        }
                    }
                }.runTaskTimer(SoulSnatcher.getPlugin(), 5L, 5L);
            }, 1L);
        }
    }
}
