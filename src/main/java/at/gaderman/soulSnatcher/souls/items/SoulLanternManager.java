package at.gaderman.soulSnatcher.souls.items;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.gui.menus.SoulLanternGUI;
import at.gaderman.soulSnatcher.souls.SoulEffects;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SoulLanternManager implements Listener {

    private static final NamespacedKey LANTERN_KEY = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_lantern");

    public static ItemStack getLantern(Player player){
        List<SoulInstance<?>> souls = SoulType.getCarriedSouls(player);

        ItemStack lantern = ItemUtils.createCustomHead("http://textures.minecraft.net/texture/" +
                "20bd20128c71210505d8062a51ae2abe0cc3fca50107f89f12d3a8d6dcfdaea1");
        lantern.editMeta(meta -> {
            meta.displayName(Component.text("Soul Lantern", TextColor.color(0x10a1e1)).decoration(TextDecoration.ITALIC, false));
            var lore = ItemUtils.applyDefaultLoreStyle(
                    Component.text("Interact to open souls GUI", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Active souls:", NamedTextColor.GRAY)
            );
            lore.addAll(souls.stream()
                    .map(soul -> Component.text("➤ ", NamedTextColor.DARK_GRAY)
                            .append(soul.soulType().displayName().decoration(TextDecoration.ITALIC, false)))
                    .toList());
            meta.lore(lore);

            meta.getPersistentDataContainer().set(LANTERN_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        });
        lantern.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
        lantern.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return lantern;
    }

    public static void updateActiveLanterns(Player player){
        PlayerInventory inv = player.getInventory();

        List<SoulInstance<?>> souls = SoulType.getCarriedSouls(player);
        if(souls.isEmpty()){
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if(item == null || item.getType().isAir()) continue;
                if(!item.getPersistentDataContainer().has(LANTERN_KEY)) continue;

                inv.setItem(i, ItemStack.of(Material.AIR));
            }
            return;
        }

        boolean foundLantern = false;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if(item == null || item.getType().isAir()) continue;
            if(!item.getPersistentDataContainer().has(LANTERN_KEY)) continue;

            inv.setItem(i, getLantern(player));
            foundLantern = true;
        }

        if(!foundLantern) {
            player.give(getLantern(player));
            player.playSound(player, Sound.BLOCK_PUMPKIN_CARVE, 1f, 0.5f);
        }
    }

    @EventHandler
    public void onPlayerClickLantern(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if(clicked != null && !clicked.getType().isAir() && clicked.getPersistentDataContainer().has(LANTERN_KEY)){
            event.setCurrentItem(getLantern(player));
        }
    }

    @EventHandler
    public void onPlaceLantern(BlockPlaceEvent event){
        ItemStack item = event.getItemInHand();
        if(item.getPersistentDataContainer().has(LANTERN_KEY))
            event.setCancelled(true);
    }

    @EventHandler
    public void onLanternInteract(PlayerInteractEvent event){
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        ItemStack item = event.getItem();
        if(item == null) return;

        if(!item.getPersistentDataContainer().has(LANTERN_KEY)) return;

        Player player = event.getPlayer();
        new SoulLanternGUI(player).openInventory(player);
        player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.25f);
    }

    private static final Set<UUID> lookingAtOrbits = new HashSet<>();

    public static boolean isLookingAtOrbits(Player player){
        return lookingAtOrbits.contains(player.getUniqueId());
    }

    @EventHandler
    public void updateHeld(EntityEquipmentChangedEvent event){
        if(!(event.getEntity() instanceof Player player)) return;

        List<EquipmentSlot> handSlots = event.getEquipmentChanges().keySet().stream()
                .filter(slot -> slot == EquipmentSlot.HAND)
                .toList();

        if(handSlots.isEmpty())
            return;

        if(lookingAtOrbits.contains(player.getUniqueId())){
            if(handSlots.stream()
                    .noneMatch(slot -> event.getEquipmentChanges().get(slot).newItem()
                            .getPersistentDataContainer()
                            .has(LANTERN_KEY))){
                SoulEffects.hideSoulOrbits(player);
                lookingAtOrbits.remove(player.getUniqueId());
            }
        } else {
            if(handSlots.stream()
                    .anyMatch(slot -> event.getEquipmentChanges().get(slot).newItem()
                            .getPersistentDataContainer()
                            .has(LANTERN_KEY))){
                SoulEffects.showSoulOrbits(player);
                lookingAtOrbits.add(player.getUniqueId());
            }
        }
    }
}
