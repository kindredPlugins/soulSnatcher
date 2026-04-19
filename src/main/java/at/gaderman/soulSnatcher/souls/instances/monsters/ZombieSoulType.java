package at.gaderman.soulSnatcher.souls.instances.monsters;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ReinforcementZombieGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.triggers.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnTargetTrigger;
import at.gaderman.soulSnatcher.utils.BlockUtils;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@AutoService(SoulType.class)
public class ZombieSoulType extends SoulType {

    private static ZombieSoulListener listener;

    public ZombieSoulType(){
        super();
        if(listener == null) {
            listener = new ZombieSoulListener();
            Bukkit.getPluginManager().registerEvents(listener, SoulSnatcher.getPlugin());
        }
    }

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

    private static final NamespacedKey REINFORCEMENT_OWNER = new NamespacedKey(SoulSnatcher.getPlugin(),
            "reinforcement_owner");
    private static final Map<Zombie, Set<LivingEntity>> reinforcementTargetMap = new LinkedHashMap<>();

    public static class ZombieSoulInstance extends SoulInstance implements OnDamageReceivedTrigger, OnDamageDealtTrigger, OnTargetTrigger {
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
        public void onTarget(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event) {
            if(isPlayerBound())
                combatTargets.add(entity);
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
//            if(event.getDamageSource().isIndirect())
//                return; //if only melee damage should call reinforcements

            if (isPlayerBound())
                combatTargets.add(damager);

            if (lastReinforcement > System.currentTimeMillis() - REINFORCEMENT_COOLDOWN || reinforcements.size() >= MAX_REINFORCEMENTS)
                return;

            lastReinforcement = System.currentTimeMillis();

            Location spawnLoc = BlockUtils.findSpreadLocation(carrier.getLocation(), 8, 3);
            Zombie zombie = carrier.getWorld().spawn(spawnLoc == null ? carrier.getLocation() : spawnLoc, Zombie.class);
            zombie.setPersistent(false);
            zombie.addScoreboardTag(SoulType.NO_SOUL_RELEASE_TAG);
            zombie.setTarget(damager);
            zombie.getAttribute(Attribute.SPAWN_REINFORCEMENTS).setBaseValue(-100);
            Bukkit.getMobGoals().addGoal(zombie, 0, new ReinforcementZombieGoal(zombie, carrier));

            reinforcements.add(zombie);
            reinforcementTargetMap.put(zombie, combatTargets);
            zombie.getPersistentDataContainer().set(REINFORCEMENT_OWNER, PersistentDataType.STRING, carrier.getUniqueId().toString());

            var zombieEquip = zombie.getEquipment();
            if(carrier.getEquipment() != null){
                var carrierEquip = carrier.getEquipment();

                zombieEquip.setHelmet(carrierEquip.getHelmet());
                zombieEquip.setHelmetDropChance(0.0f);
                zombieEquip.setChestplate(carrierEquip.getChestplate());
                zombieEquip.setChestplateDropChance(0.0f);
                zombieEquip.setLeggings(carrierEquip.getLeggings());
                zombieEquip.setLeggingsDropChance(0.0f);
                zombieEquip.setBoots(carrierEquip.getBoots());
                zombieEquip.setBootsDropChance(0.0f);
                zombieEquip.setItemInMainHand(carrierEquip.getItemInMainHand());
                zombieEquip.setItemInMainHandDropChance(0.0f);
                zombieEquip.setItemInOffHand(carrierEquip.getItemInOffHand());
                zombieEquip.setItemInOffHandDropChance(0.0f);
            }

            if (carrier instanceof Player player) {
                zombieEquip.setHelmet(ItemUtils.getHeadOfPlayer(player));
            }else{
                var potSoulConnection = SoulRegistry.getInstance().getSoul(carrier.getType());
                potSoulConnection.ifPresent(soulType -> zombieEquip.setHelmet(soulType.getRepresentativeSkull()));
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!zombie.isValid() || !carrier.isValid() || !zombie.getWorld().equals(carrier.getWorld())) {
                        reinforcements.remove(zombie);
                        cancel();

                        if (!carrier.isValid() || !zombie.getWorld().equals(carrier.getWorld())) {
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
                                nextTarget = combatTargets.stream()
                                        .filter(target -> !target.equals(zombie) &&
                                                !target.getPersistentDataContainer().getOrDefault(REINFORCEMENT_OWNER, PersistentDataType.STRING, "").equals(carrier.getUniqueId().toString()))
                                        .findFirst().orElse(null);
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

    /**
     * Used to make reinforcement zombies behave like actual reinforcements without having to unnecessarily impact the TriggerListener
     */
    private static class ZombieSoulListener implements Listener {

        @EventHandler
        public void onReinforcementTarget(EntityTargetLivingEntityEvent event){
            if(event.getTarget() == null || !(event.getEntity() instanceof Zombie zombie)) return;
            if(!reinforcementTargetMap.containsKey(zombie)) return;

            if(!reinforcementTargetMap.get(zombie).contains(event.getTarget()))
                event.setCancelled(true);
        }

        @EventHandler
        public void onProjectileHitZombie(ProjectileHitEvent event){
            if(!(event.getHitEntity() instanceof Zombie zombie)) return;
            if(!zombie.getPersistentDataContainer().has(REINFORCEMENT_OWNER)) return;
            if(!(event.getEntity().getShooter() instanceof LivingEntity shooter)) return;

            String ownerUUID = zombie.getPersistentDataContainer().get(REINFORCEMENT_OWNER, PersistentDataType.STRING);
            if(shooter.getUniqueId().toString().equals(ownerUUID))
                event.setCancelled(true);
        }

    }
}
