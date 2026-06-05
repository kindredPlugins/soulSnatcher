package at.gaderman.soulSnatcher.gui.menus;

import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.souls.SoulEffects;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SoulLanternGUI extends ActionInventory {

    private final Player player;
    private final List<SoulInstance> carriedSouls;

    public SoulLanternGUI(Player player) {
        super(Component.text("Soul Lantern", NamedTextColor.BLUE));

        this.player = player;
        this.carriedSouls = SoulType.getCarriedSouls(player);
    }

    @Override
    protected int calculateSize() {
        return 36;
    }

    @Override
    protected void initializeInventory(int size, Component inventoryName) {
        super.initializeInventory(size, inventoryName);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, getFillItem());
        }

        int startIndex = 11;
        for (int i = 0; i < carriedSouls.size(); i++) {
            SoulInstance soul = carriedSouls.get(i);
            int soulSlot = startIndex + (i * 4);

            inventory.setItem(soulSlot, soul.soulType().itemRepresentation());
            inventory.setItem(soulSlot + 9, getRemoveItem(soul));
            defineInventoryAction(soulSlot + 9, event -> remove(soul));
        }

        for (int i = carriedSouls.size(); i < SoulType.MAX_BOUND_SOULS; i++) {
            int soulSlot = startIndex + (i * 4);

            inventory.setItem(soulSlot, getNoSoulItem());
        }
    }

    private void remove(SoulInstance replaced){
        replaced.soulType().removeSoul(player);
        SoulEffects.discardSoulRewardEffect(player.getLocation().add(0, 1, 0));

        inventory.close();
    }

    private ItemStack getRemoveItem(SoulInstance soul) {
        ItemStack item = ItemStack.of(Material.BARRIER);
        item.editMeta(meta -> {
                    meta.itemName(Component.text("Remove ", NamedTextColor.DARK_RED)
                            .append(soul.soulType().displayName()));
                    meta.lore(ItemUtils.applyDefaultLoreStyle(
                            Component.text("Completely ")
                                    .append(Component.text("removes ", NamedTextColor.RED))
                                    .append(Component.text("this soul from yourself", NamedTextColor.WHITE))
                    ));
                }
        );
        return item;
    }

    private ItemStack getNoSoulItem(){
        ItemStack item = ItemStack.of(Material.GRAY_DYE);
        item.editMeta(meta -> {
            meta.itemName(Component.text("Soul Slot empty", NamedTextColor.GRAY));
            meta.lore(ItemUtils.applyDefaultLoreStyle(
                    Component.text("Bind a soul by killing an infused mob")
            ));
        });
        return item;
    }
}
