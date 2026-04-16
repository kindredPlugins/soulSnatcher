package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.souls.triggers.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnDamageReceivedTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

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
        if (!(event.getDamager() instanceof LivingEntity damager)) return;

        List<SoulInstance> targetSouls = SoulType.getCarriedSouls(entity);
        targetSouls.stream()
                .filter(soul -> soul instanceof OnDamageReceivedTrigger)
                .map(soul -> (OnDamageReceivedTrigger) soul)
                .forEach(trigger -> {
                    trigger.onDamageReceivedByEntity(entity, damager, event);
                });

        List<SoulInstance> damagerSouls = SoulType.getCarriedSouls(damager);
        damagerSouls.stream()
                .filter(soul -> soul instanceof OnDamageReceivedTrigger)
                .map(soul -> (OnDamageDealtTrigger) soul)
                .forEach(trigger -> {
                    trigger.onDamageDealt(damager, entity, event);
                });
    }

}
