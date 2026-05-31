package at.gaderman.soulSnatcher.souls.triggers.input;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;

public interface OnPlayerJumpTrigger {
    void onPlayerJump(Player carrier, PlayerJumpEvent event);
}
