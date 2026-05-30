package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.souls.triggers.*;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityLaunchProjectileTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityShootBowTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnProjectileHitTrigger;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

import java.util.List;

public class TriggerListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onDamageReceivedTrigger(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnDamageReceivedTrigger)
                .map(soul -> (OnDamageReceivedTrigger) soul)
                .forEach(trigger -> {
                    trigger.onDamageReceived(entity, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageReceivedByEntityTrigger(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        DamageSource source = event.getDamageSource();
        if (!(source.getCausingEntity() instanceof LivingEntity damager)) return;

        List<SoulInstance> targetSouls = SoulType.getCarriedSouls(entity);
        targetSouls.stream()
                .filter(soul -> soul instanceof OnDamageReceivedTrigger)
                .map(soul -> (OnDamageReceivedTrigger) soul)
                .forEach(trigger -> {
                    trigger.onDamageReceivedByEntity(entity, damager, event);
                });

        List<SoulInstance> damagerSouls = SoulType.getCarriedSouls(damager);
        damagerSouls.stream()
                .filter(soul -> soul instanceof OnDamageDealtTrigger)
                .map(soul -> (OnDamageDealtTrigger) soul)
                .forEach(trigger -> {
                    trigger.onDamageDealt(damager, entity, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamageTrigger(PlayerItemDamageEvent event){
        Player player = event.getPlayer();

        List<SoulInstance> souls = SoulType.getCarriedSouls(player);
        souls.stream()
                .filter(soul -> soul instanceof OnItemDamageTrigger)
                .map(soul -> (OnItemDamageTrigger) soul)
                .forEach(trigger -> {
                    trigger.onItemDamage(player, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractTrigger(PlayerInteractEvent event){
        Player player = event.getPlayer();

        List<SoulInstance> souls = SoulType.getCarriedSouls(player);
        souls.stream()
                .filter(soul -> soul instanceof OnPlayerInteractTrigger)
                .map(soul -> (OnPlayerInteractTrigger) soul)
                .forEach(trigger -> {
                    trigger.onPlayerInteract(player, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPotionEffectTrigger(EntityPotionEffectEvent event){
        if(!(event.getEntity() instanceof LivingEntity entity)) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnEntityPotionEffectTrigger)
                .map(soul -> (OnEntityPotionEffectTrigger) soul)
                .forEach(trigger -> {
                    trigger.onEntityPotionEffect(entity, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onJumpTrigger(PlayerJumpEvent event){
        Player player = event.getPlayer();

        List<SoulInstance> souls = SoulType.getCarriedSouls(player);
        souls.stream()
                .filter(soul -> soul instanceof OnPlayerJumpTrigger)
                .map(soul -> (OnPlayerJumpTrigger) soul)
                .forEach(trigger -> {
                    trigger.onPlayerJump(player, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event){
        if(event.getTarget() == null || !(event.getEntity() instanceof LivingEntity provoker)) return;

        LivingEntity target = event.getTarget();
        List<SoulInstance> souls = SoulType.getCarriedSouls(target);
        souls.stream()
                .filter(soul -> soul instanceof OnTargetTrigger)
                .map(soul -> (OnTargetTrigger) soul)
                .forEach(trigger -> {
                    trigger.onBeingTargeted(target, provoker, event);
                });

        List<SoulInstance> carrierSouls = SoulType.getCarriedSouls(provoker);
        carrierSouls.stream()
                .filter(soul -> soul instanceof OnTargetTrigger)
                .map(soul -> (OnTargetTrigger) soul)
                .forEach(trigger -> {
                    trigger.onCarrierTarget(provoker, target, event);
                });
    }

    // PROJECTILES

    @EventHandler
    public void onEntityShootBowTrigger(EntityShootBowEvent event){
        LivingEntity entity = event.getEntity();

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnEntityShootBowTrigger)
                .map(soul -> (OnEntityShootBowTrigger) soul)
                .forEach(trigger -> {
                    trigger.onEntityShootBow(entity, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityLaunchProjectileTrigger(ProjectileLaunchEvent event){
        if(!(event.getEntity().getShooter() instanceof LivingEntity entity)) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnEntityLaunchProjectileTrigger)
                .map(soul -> (OnEntityLaunchProjectileTrigger) soul)
                .forEach(trigger -> {
                    trigger.onEntityLaunchProjectile(entity, event.getEntity(), event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event){
        if(!(event.getEntity().getShooter() instanceof LivingEntity entity)) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnProjectileHitTrigger)
                .map(soul -> (OnProjectileHitTrigger) soul)
                .forEach(trigger -> {
                    trigger.onProjectileHit(entity, event.getEntity(), event);
                });
    }
}
