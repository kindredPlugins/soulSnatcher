package at.gaderman.soulSnatcher.souls.triggers;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public interface OnRegainHealthTrigger {
    void onRegainHealth(LivingEntity carrier, EntityRegainHealthEvent event);
}
