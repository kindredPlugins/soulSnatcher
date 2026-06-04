package at.gaderman.soulSnatcher.souls.triggers.interact;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public interface OnConsumeItemTrigger {
    void onConsumeItem(Player carrier, ItemStack item, PlayerItemConsumeEvent event);
}
