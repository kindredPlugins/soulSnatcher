package at.gaderman.soulSnatcher.souls.triggers.input;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityToggleSwimEvent;

public interface OnSwimToggleTrigger {
    void onSwimToggle(LivingEntity carrier, EntityToggleSwimEvent event);
}
