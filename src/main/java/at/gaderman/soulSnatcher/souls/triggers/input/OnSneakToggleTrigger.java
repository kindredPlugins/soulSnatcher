package at.gaderman.soulSnatcher.souls.triggers.input;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public interface OnSneakToggleTrigger {
    void onSneakToggle(Player carrier, PlayerToggleSneakEvent event);
}
