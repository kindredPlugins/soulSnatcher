package at.gaderman.soulSnatcher.souls.instances.combat.targeting;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.triggers.OnTargetTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TargetTrackerSoulInstance extends SoulInstance implements OnDamageReceivedTrigger, OnDamageDealtTrigger, OnTargetTrigger {
    protected TargetTrackerSoulInstance(LivingEntity carrier, SoulType soulType) {
        super(carrier, soulType);

        combatTargets = carrier.getWorld().getNearbyLivingEntities(carrier.getLocation(), 50).stream()
                .filter(target -> (target instanceof Mob mob && carrier.equals(mob.getTarget())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    protected Set<LivingEntity> combatTargets;

    protected void addCombatTarget(LivingEntity target) {
        combatTargets.add(target);
    }

    @Override
    public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
        addCombatTarget(target);
    }

    @Override
    public void onBeingTargeted(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event) {
        Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
            if (event.isCancelled() || !carrier.isValid() || !entity.isValid()) return;

            if (entity.getPersistentDataContainer().getOrDefault(ZombieSoulType.REINFORCEMENT_OWNER, PersistentDataType.STRING,
                    "").equals(carrier.getUniqueId().toString()))
                return;

            addCombatTarget(entity);
        }, 1L);
    }

    @Override
    public void onCarrierTarget(LivingEntity carrier, LivingEntity target, EntityTargetLivingEntityEvent event) {
        Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
            if (event.isCancelled() || !carrier.isValid() || !target.isValid()) return;

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