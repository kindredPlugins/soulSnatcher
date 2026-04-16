package at.gaderman.soulSnatcher.souls.instances.monsters;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.triggers.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.BlockUtils;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@AutoService(SoulType.class)
public class ZombieSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new ZombieSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "zombie_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.ZOMBIE;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "783aaaee22868cafdaa1f6f4a0e56b0fdb64cd0aeaabd6e83818c312ebe66437";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Zombie Soul", NamedTextColor.DARK_GREEN);
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When being hit summons a ")
                        .append(Component.text("reinforcement zombie", NamedTextColor.AQUA)),
                Component.text("nearby who will aid you in combat")
        );
    }

    public static class ZombieSoulInstance extends SoulInstance implements OnDamageReceivedTrigger, OnDamageDealtTrigger {
        protected ZombieSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);
        }

        private static final long REINFORCEMENT_COOLDOWN = 2000;
        private long lastReinforcement;
        private static final int MAX_REINFORCEMENTS = 3;
        private final Set<Zombie> reinforcements = new LinkedHashSet<>();

        private final Set<LivingEntity> combatTargets = new LinkedHashSet<>();

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if (isPlayerBound())
                combatTargets.add(target);
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            if (isPlayerBound())
                combatTargets.add(damager);

            if (lastReinforcement > System.currentTimeMillis() - REINFORCEMENT_COOLDOWN || reinforcements.size() >= MAX_REINFORCEMENTS)
                return;

            lastReinforcement = System.currentTimeMillis();

            Location spawnLoc = BlockUtils.findSpreadLocation(carrier.getLocation(), 8, 16);
            Zombie zombie = carrier.getWorld().spawn(spawnLoc == null ? carrier.getLocation() : spawnLoc, Zombie.class);
            zombie.setPersistent(false);
            zombie.addScoreboardTag(SoulType.NO_SOUL_RELEASE_TAG);
            zombie.setTarget(damager);
            reinforcements.add(zombie);

            if (carrier instanceof Player player) {
                var equipment = zombie.getEquipment();
                equipment.setHelmet(ItemUtils.getHeadOfPlayer(player));
                equipment.setHelmetDropChance(0.0f);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!zombie.isValid() || !carrier.isValid()) {
                        reinforcements.remove(zombie);
                        cancel();

                        if (!carrier.isValid()) {
                            zombie.remove();

                            zombie.getWorld().spawnParticle(Particle.WHITE_SMOKE, zombie.getLocation().add(0, 0.5, 0), 30, 0, 0, 0, 0.1);
                            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_DEATH, 1f, 0.5f);
                        }
                        return;
                    }

                    if (zombie.getTarget() == null || zombie.getTarget().equals(carrier)) {
                        if (isPlayerBound()) {
                            LivingEntity nextTarget;
                            while (true) {
                                nextTarget = combatTargets.stream().findFirst().orElse(null);
                                if (nextTarget == null) break;
                                if (nextTarget.isValid()) break;
                                combatTargets.remove(nextTarget);
                            }
                            zombie.setTarget(nextTarget);
                        } else if (carrier instanceof Mob mob) {
                            LivingEntity target = mob.getTarget();
                            if (target != null && target.equals(zombie)) {
                                zombie.setTarget(null);
                            } else {
                                zombie.setTarget(mob.getTarget());
                            }
                        }

                    }
                }
            }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, 5L);
        }

    }
}
