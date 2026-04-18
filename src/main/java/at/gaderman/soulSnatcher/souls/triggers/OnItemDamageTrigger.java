package at.gaderman.soulSnatcher.souls.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;

public interface OnItemDamageTrigger {
    void onItemDamage(Player player, PlayerItemDamageEvent event);
}
