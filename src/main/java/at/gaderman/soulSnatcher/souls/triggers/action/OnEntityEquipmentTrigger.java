package at.gaderman.soulSnatcher.souls.triggers.action;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.LivingEntity;

public interface OnEntityEquipmentTrigger {
    void onEntityEquipmentChange(LivingEntity carrier, EntityEquipmentChangedEvent event);
}
