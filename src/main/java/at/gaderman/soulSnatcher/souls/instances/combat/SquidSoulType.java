package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.instances.combat.targeting.TargetTrackerSoulInstance;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@AutoService(SoulType.class)
public class SquidSoulType extends SoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new SquidSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "squid_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SQUID;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "464bdc6f600656511bef596c1a16aab1d3f5dbaae8bee19d5c04de0db21ce92c";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Squid Soul", TextColor.color(0x1b3243));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(

        );
    }

    public static class SquidSoulInstance extends TargetTrackerSoulInstance {
        protected SquidSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);
        }

        private long lastInkBurst;
        private static final long INK_COOLDOWN = 8000;

        private static final int INK_DURATION = 60;

        private static final NamespacedKey INK_DEBUFF = new NamespacedKey(SoulSnatcher.getPlugin(), "ink_debuff");

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            super.onDamageReceivedByEntity(carrier, damager, event);

            if (lastInkBurst > System.currentTimeMillis() - INK_COOLDOWN) return;

            lastInkBurst = System.currentTimeMillis();

            combatTargets = combatTargets.stream().filter(Entity::isValid).collect(Collectors.toSet());

            carrier.getWorld().spawnParticle(Particle.SQUID_INK, carrier.getLocation().add(0, 1, 0), 50,
                    0, 0, 0, 0.1);
            carrier.getWorld().spawnParticle(Particle.LARGE_SMOKE, carrier.getLocation().add(0, 1, 0), 10,
                    0, 0, 0, 0.1);
            carrier.getWorld().playSound(carrier, Sound.ENTITY_SQUID_HURT, 1f, 1f);

            carrier.getWorld().getNearbyLivingEntities(carrier.getLocation(), 5).forEach(target -> {
                if (!combatTargets.contains(target)) return;

                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, INK_DURATION, 0, true));

                new BukkitRunnable(){
                    int ticks = 0;

                    @Override
                    public void run() {
                        if(!target.isValid() || ticks >= INK_DURATION){
                            cancel();
                            return;
                        }

                        target.getWorld().spawnParticle(Particle.SQUID_INK, target.getEyeLocation(), 1,
                                0.2, 0.2, 0.2, 0);

                        ticks += 2;
                    }
                }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, 2L);

                if(target instanceof Mob mob){
                    var followRange = mob.getAttribute(Attribute.FOLLOW_RANGE);
                    if(followRange != null)
                        followRange.addModifier(new AttributeModifier(INK_DEBUFF, -0.85, AttributeModifier.Operation.ADD_SCALAR));

                    var movSpeed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
                    if(movSpeed != null)
                        movSpeed.addModifier(new AttributeModifier(INK_DEBUFF, -0.2, AttributeModifier.Operation.ADD_SCALAR));

                    mob.getPersistentDataContainer().set(INK_DEBUFF, PersistentDataType.STRING, carrier.getUniqueId().toString());

                    SoulSnatcher.getPlugin().registerDelayedTask(() -> {
                        if(mob.isDead()) return;

                        if(!mob.getPersistentDataContainer().getOrDefault(INK_DEBUFF, PersistentDataType.STRING, "")
                                .equals(carrier.getUniqueId().toString())) return;

                        if(followRange != null) followRange.removeModifier(INK_DEBUFF);
                        if(movSpeed != null) movSpeed.removeModifier(INK_DEBUFF);

                        mob.getPersistentDataContainer().remove(INK_DEBUFF);
                    }, INK_DURATION);
                }
            });
        }
    }
}
