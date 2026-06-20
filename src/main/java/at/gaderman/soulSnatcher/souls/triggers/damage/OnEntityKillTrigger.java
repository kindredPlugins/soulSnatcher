package at.gaderman.soulSnatcher.souls.triggers.damage;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public interface OnEntityKillTrigger {
    void onEntityKillTrigger(Player player, LivingEntity killed, EntityDeathEvent event);
}
