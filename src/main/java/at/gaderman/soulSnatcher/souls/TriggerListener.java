package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.souls.triggers.*;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

import java.util.List;

public class TriggerListener implements Listener {
    @EventHandler
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

    @EventHandler
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

    @EventHandler
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

    @EventHandler
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

    @EventHandler
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

    @EventHandler
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

}
