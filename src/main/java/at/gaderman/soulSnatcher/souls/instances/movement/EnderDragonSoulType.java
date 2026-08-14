package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.souls.triggers.input.OnEntityToggleGlideTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class EnderDragonSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<EnderDragonSoulType> create(LivingEntity carrier) {
        return new EnderDragonSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "ender_dragon_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.ENDER_DRAGON;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "f68c1c079a7ffb36f48dd7150355e3e0b7f68dd605e6f8847313c360cf61e0c";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component displayName() {
        return ItemUtils.gradient("Ender Dragon Soul", TextColor.color(0x6c76ab), NamedTextColor.DARK_PURPLE);
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(

        );
    }

    //region Config Values

//    private static final String JUMP_COOLDOWN_CONFIG_ID = "jump_cooldown";
//
//    private final ConfigOption<Integer> jumpCooldown = configOption(JUMP_COOLDOWN_CONFIG_ID, 50, FileConfiguration::getInt, value -> Math.max(value, 0));
//
//    @Override
//    public Map<String, String> extraConfigPathCommentMap() {
//        return Map.of(
//                JUMP_COOLDOWN_CONFIG_ID, "Cooldown for being able to do the wind charge jump after ground has been touched in ticks (20 ticks = 1 second)"
//        );
//    }

    //endregion

    public static class EnderDragonSoulInstance extends SoulInstance<EnderDragonSoulType> implements OnEntityToggleGlideTrigger, OnDamageReceivedTrigger {
        protected EnderDragonSoulInstance(LivingEntity carrier, EnderDragonSoulType soulType) {
            super(carrier, soulType);
        }

        private static NamespacedKey SOUL_BREATH = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_breath");

        private ScheduledTask flyingTask;
        private boolean dragonFlight;

        private long lastLandExplosion;
        private static final int LAND_EXPLOSION_COOLDOWN = 1000;

        @Override
        public void onToggleGlide(LivingEntity carrier, @NotNull EntityToggleGlideEvent event) {
            if (!event.isGliding() || flyingTask != null)
                return;

            flyingTask = carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), (task) -> {
                if (!carrier.isGliding()) {
                    dragonFlight = false;
                    flyingTask = null;
                    task.cancel();
                    return;
                }

                if (!dragonFlight) {
                    double magnitude = carrier.getVelocity().lengthSquared();

                    if (magnitude < 1.1) {
                        dragonFlight = false;
                        return;
                    }

                    dragonFlight = true;
                    carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);

                    carrier.setVelocity(carrier.getVelocity().multiply(1.5));
                } else {
                    carrier.setVelocity(carrier.getVelocity().multiply(1.005));
                }

                carrier.getWorld().spawnParticle(Particle.DRAGON_BREATH, carrier.getLocation(), 10, 2, 1, 2, 0.2, 1f);

                if (Bukkit.getCurrentTick() % 10 == 0)
                    carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
            }, () -> flyingTask = null, 1L, 1L);
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if(!dragonFlight)
                return;

            if (event.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL
                    && (event.getCause() != EntityDamageEvent.DamageCause.FALL || !carrier.isGliding()))
                return;

            if(lastLandExplosion > System.currentTimeMillis() - LAND_EXPLOSION_COOLDOWN)
                return;

            lastLandExplosion = System.currentTimeMillis();

            double damage = event.getDamage();
            event.setDamage(damage * 0.25);

            if (carrier instanceof Player player)
                player.sendMessage(Component.text("You dealt")
                        .append(Component.text(Math.min(damage, 20), NamedTextColor.GOLD))
                );

            carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);
            carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1f);
            carrier.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, carrier.getLocation(), 1);

            double explosionDamage = Math.min(damage, 20);
            double radius = 4;
            carrier.getWorld().getNearbyLivingEntities(carrier.getLocation(), radius).forEach(target -> {
                if (target.equals(carrier))
                    return;

                double factor = 1 - target.getLocation().distanceSquared(carrier.getLocation()) / radius;
                target.damage(explosionDamage * factor, DamageSource.builder(DamageType.EXPLOSION)
                        .withDirectEntity(carrier)
                        .withCausingEntity(carrier)
                        .build());
            });

            carrier.getWorld().spawn(carrier.getLocation(), AreaEffectCloud.class, breath -> {
                breath.setParticle(Particle.DRAGON_BREATH, 1f);
                breath.setBasePotionType(PotionType.HARMING);
                breath.setOwnerUniqueId(carrier.getUniqueId());

                breath.setDuration(100);
                breath.setRadius(2);
                breath.setRadiusPerTick(0.025f);

                breath.getPersistentDataContainer().set(SOUL_BREATH, PersistentDataType.STRING, carrier.getUniqueId().toString());
            });
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            if (!(event.getDamager() instanceof AreaEffectCloud breath))
                return;

            String owner = breath.getPersistentDataContainer().getOrDefault(SOUL_BREATH, PersistentDataType.STRING, "");
            if (!owner.equals(carrier.getUniqueId().toString()))
                return;

            event.setCancelled(true);
        }
    }
}
