package at.gaderman.soulSnatcher.souls.triggers.interact;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public interface OnPlayerInteractEntityTrigger {
    void onPlayerInteractEntity(Player player, Entity entity, PlayerInteractEntityEvent event);
}
