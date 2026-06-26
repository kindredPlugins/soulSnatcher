package at.gaderman.soulSnatcher.souls.triggers.interact;

import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.entity.Player;

public interface OnStopUsingItemTrigger {
    void onStopUsingItem(Player player, PlayerStopUsingItemEvent event);
}
