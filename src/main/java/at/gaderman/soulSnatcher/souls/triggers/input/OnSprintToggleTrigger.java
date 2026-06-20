package at.gaderman.soulSnatcher.souls.triggers.input;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public interface OnSprintToggleTrigger {
    void onSprintToggle(Player carrier, PlayerToggleSprintEvent event);
}
