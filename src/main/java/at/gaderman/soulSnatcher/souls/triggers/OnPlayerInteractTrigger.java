package at.gaderman.soulSnatcher.souls.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public interface OnPlayerInteractTrigger {
    void onPlayerInteract(Player player, PlayerInteractEvent event);
}
