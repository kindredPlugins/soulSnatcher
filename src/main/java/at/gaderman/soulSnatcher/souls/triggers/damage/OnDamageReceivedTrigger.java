package at.gaderman.soulSnatcher.souls.triggers.damage;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public interface OnDamageReceivedTrigger {

    void onDamageReceived(LivingEntity carrier, EntityDamageEvent event);
    void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event);

    int FIRST_PRIORITY = 0;

    /// Used when soul interconnections can change one another.
    /// E.g. some abilities completely cancel damage received so they should always be run before modifying events
    /// 0 - event can lead to damage cancellation and needs to run first
    /// 10 - order does not matter, or it needs to run last
    default int damageReceivedPriority(){
        return 10;
    }
}
