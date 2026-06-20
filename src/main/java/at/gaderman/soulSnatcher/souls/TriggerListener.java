package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.souls.triggers.OnEntityEquipmentTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnEntityPotionEffectTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnItemDamageTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnTargetTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnEntityKillTrigger;
import at.gaderman.soulSnatcher.souls.triggers.input.OnPlayerJumpTrigger;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSneakToggleTrigger;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSprintToggleTrigger;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSwimToggleTrigger;
import at.gaderman.soulSnatcher.souls.triggers.interact.OnConsumeItemTrigger;
import at.gaderman.soulSnatcher.souls.triggers.interact.OnPlayerInteractEntityTrigger;
import at.gaderman.soulSnatcher.souls.triggers.interact.OnPlayerInteractTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityLaunchProjectileTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityShootBowTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnHitByProjectileTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnProjectileHitTrigger;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

import java.util.List;

public class TriggerListener implements Listener {

    //region Damage

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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityKillTrigger(EntityDeathEvent event){
        LivingEntity killed = event.getEntity();
        Player player = killed.getKiller();

        if(player == null)
            return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(player);
        souls.stream()
                .filter(soul -> soul instanceof OnEntityKillTrigger)
                .map(soul -> (OnEntityKillTrigger) soul)
                .forEach(trigger -> {
                    trigger.onEntityKillTrigger(player, killed, event);
                });
    }

    //endregion

    //region Interactions

    @EventHandler(ignoreCancelled = true)
    public void onItemDamageTrigger(PlayerItemDamageEvent event) {
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
    public void onPlayerInteractTrigger(PlayerInteractEvent event) {
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
    public void onPlayerInteractEntityTrigger(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        List<SoulInstance> souls = SoulType.getCarriedSouls(player);
        souls.stream()
                .filter(soul -> soul instanceof OnPlayerInteractEntityTrigger)
                .map(soul -> (OnPlayerInteractEntityTrigger) soul)
                .forEach(trigger -> {
                    trigger.onPlayerInteractEntity(player, event.getRightClicked(), event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsumeItemTrigger(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        List<SoulInstance> souls = SoulType.getCarriedSouls(player);
        souls.stream()
                .filter(soul -> soul instanceof OnConsumeItemTrigger)
                .map(soul -> (OnConsumeItemTrigger) soul)
                .forEach(trigger -> {
                    trigger.onConsumeItem(player, event.getItem(), event);
                });
    }

    //endregion

    //region Projectiles

    @EventHandler
    public void onEntityShootBowTrigger(EntityShootBowEvent event) {
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
    public void onEntityLaunchProjectileTrigger(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof LivingEntity entity)) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnEntityLaunchProjectileTrigger)
                .map(soul -> (OnEntityLaunchProjectileTrigger) soul)
                .forEach(trigger -> {
                    trigger.onEntityLaunchProjectile(entity, event.getEntity(), event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof LivingEntity target) {
            List<SoulInstance> souls = SoulType.getCarriedSouls(target);
            souls.stream()
                    .filter(soul -> soul instanceof OnHitByProjectileTrigger)
                    .map(soul -> (OnHitByProjectileTrigger) soul)
                    .forEach(trigger -> {
                        trigger.onHitByProjectile(target, event.getEntity(), event);
                    });
        }

        if (!(event.getEntity().getShooter() instanceof LivingEntity entity)) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnProjectileHitTrigger)
                .map(soul -> (OnProjectileHitTrigger) soul)
                .forEach(trigger -> {
                    trigger.onProjectileHit(entity, event.getEntity(), event);
                });
    }

    //endregion

    //region Inputs

    @EventHandler(ignoreCancelled = true)
    public void onJumpTrigger(PlayerJumpEvent event) {
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
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        List<SoulInstance> souls = SoulType.getCarriedSouls(player);
        souls.stream()
                .filter(soul -> soul instanceof OnSneakToggleTrigger)
                .map(soul -> (OnSneakToggleTrigger) soul)
                .forEach(trigger -> {
                    trigger.onSneakToggle(player, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();

        List<SoulInstance> souls = SoulType.getCarriedSouls(player);
        souls.stream()
                .filter(soul -> soul instanceof OnSprintToggleTrigger)
                .map(soul -> (OnSprintToggleTrigger) soul)
                .forEach(trigger -> {
                    trigger.onSprintToggle(player, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSwim(EntityToggleSwimEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnSwimToggleTrigger)
                .map(soul -> (OnSwimToggleTrigger) soul)
                .forEach(trigger -> {
                    trigger.onSwimToggle(entity, event);
                });
    }

    //endregion

    //region Others

    @EventHandler(ignoreCancelled = true)
    public void onEntityPotionEffectTrigger(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnEntityPotionEffectTrigger)
                .map(soul -> (OnEntityPotionEffectTrigger) soul)
                .forEach(trigger -> {
                    trigger.onEntityPotionEffect(entity, event);
                });
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() == null || !(event.getEntity() instanceof LivingEntity provoker)) return;

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

    @EventHandler
    public void onEquipmentChange(EntityEquipmentChangedEvent event){
        LivingEntity entity = event.getEntity();

        List<SoulInstance> souls = SoulType.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnEntityEquipmentTrigger)
                .map(soul -> (OnEntityEquipmentTrigger) soul)
                .forEach(trigger -> {
                    trigger.onEntityEquipmentChange(entity, event);
                });
    }

    //endregion
}
