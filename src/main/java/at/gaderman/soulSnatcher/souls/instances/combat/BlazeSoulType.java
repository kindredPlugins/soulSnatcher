package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.OnTargetTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(SoulType.class)
public class BlazeSoulType extends SoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new BlazeSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "blaze_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.BLAZE;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "b20657e24b56e1b2f8fc219da1de788c0c24f36388b1a409d0cd2d8dba44aa3b";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Blaze Soul", TextColor.color(0xfc9600));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(

        );
    }

    public static class BlazeSoulInstance extends SoulInstance implements OnDamageReceivedTrigger, OnDamageDealtTrigger, OnTargetTrigger {
        protected BlazeSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);

            combatTargets = carrier.getWorld().getNearbyLivingEntities(carrier.getLocation(), 50).stream()
                    .filter(target -> (target instanceof Mob mob && carrier.equals(mob.getTarget())))
                    .collect(Collectors.toSet());
            if(!combatTargets.isEmpty())
                auraTask = createAuraTask();
        }

        private static final int AURA_TIMEOUT = 30000;
        private static final float AURA_RANGE = 2.5f;
        private static final float AURA_DAMAGE = 6;

        private long lastAuraHit;
        private static final int AURA_HIT_COOLDOWN = 2000;

        private BukkitTask auraTask;
        private Set<LivingEntity> combatTargets = new LinkedHashSet<>();

        private BukkitTask createAuraTask() {
            lastAuraHit = System.currentTimeMillis();

            return new BukkitRunnable() {
                final LivingEntity carrier = carrier();

                @Override
                public void run() {
                    boolean isActive = lastAuraHit < System.currentTimeMillis() - AURA_HIT_COOLDOWN;

                    if (isActive) {
                        carrier.getWorld().spawnParticle(Particle.FLAME, carrier.getLocation().add(0, 1, 0),
                                1, 0.1, 0.5, 0.1, 0.01);
                    }

                    if (combatTargets.isEmpty()){
                        extinguish();
                        return;
                    }

                    if (Bukkit.getCurrentTick() % 10 == 0)
                        combatTargets = combatTargets.stream().filter(LivingEntity::isValid).collect(Collectors.toSet());

                    if (isActive) {
                        for (LivingEntity target : carrier.getWorld().getNearbyLivingEntities(carrier.getLocation(), AURA_RANGE)) {
                            if (target.getNoDamageTicks() != 0 || !combatTargets.contains(target)) continue;

                            lastAuraHit = System.currentTimeMillis();

                            target.damage(AURA_DAMAGE, DamageSource.builder(DamageType.MOB_ATTACK)
                                    .withDirectEntity(carrier)
                                    .build());

                            Location hitLoc = target.getEyeLocation().add(0, -0.2, 0);
                            target.getWorld().spawnParticle(Particle.LAVA, hitLoc, 30);
                            target.getWorld().spawnParticle(Particle.FLAME, hitLoc, 30, 0.3, 0.3, 0.3, 0.01);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 1f, 1.5f);
                            break;
                        }
                    }

                    if (lastAuraHit < System.currentTimeMillis() - AURA_TIMEOUT) {
                        extinguish();
                    }
                }

                private void extinguish(){
                    combatTargets.clear();
                    carrier.getWorld().spawnParticle(Particle.SMOKE, carrier.getLocation().add(0, 1, 0),
                            30, 0.5, 0.5, 0.5, 0.1);
                    carrier.getWorld().playSound(carrier, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 0.2f);

                    auraTask = null;
                    cancel();
                }
            }.runTaskTimer(SoulSnatcher.getPlugin(), 1L, 1L);
        }

        @Override
        protected void cleanUp() {
            if (this.auraTask != null)
                this.auraTask.cancel();
        }

        private void addCombatTarget(LivingEntity target) {
            combatTargets.add(target);

            if (this.auraTask == null)
                auraTask = createAuraTask();
        }

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            addCombatTarget(target);
        }

        @Override
        public void onBeingTargeted(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event) {
            Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
                if(event.isCancelled() || !carrier.isValid() || !entity.isValid()) return;

                if(entity.getPersistentDataContainer().getOrDefault(ZombieSoulType.REINFORCEMENT_OWNER, PersistentDataType.STRING,
                        "").equals(carrier.getUniqueId().toString()))
                    return;

                addCombatTarget(entity);
            }, 1L);
        }

        @Override
        public void onCarrierTarget(LivingEntity carrier, LivingEntity target, EntityTargetLivingEntityEvent event) {
            Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
                if(event.isCancelled() || !carrier.isValid() || !target.isValid()) return;

                addCombatTarget(target);
            }, 1L);
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            addCombatTarget(damager);
        }
    }
}
