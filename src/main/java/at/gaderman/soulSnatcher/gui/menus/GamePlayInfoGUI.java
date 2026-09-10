package at.gaderman.soulSnatcher.gui.menus;

import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.souls.items.SoulLanternManager;
import at.gaderman.soulSnatcher.souls.items.SoulVialManager;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class GamePlayInfoGUI extends ActionInventory {

    public GamePlayInfoGUI() {
        super(27, MenuLanguageDefinition.GAMEPLAY_INFO_TITLE.getSingle().color(NamedTextColor.GOLD));
    }

    @Override
    protected void initializeInventory(int size, Component inventoryName) {
        super.initializeInventory(size, inventoryName);

        fillWithFillItem();

        inventory.setItem(10, getSoulRelease());
        inventory.setItem(12, getInfusion());
        inventory.setItem(14, getBinding());
        inventory.setItem(16, getVial());

        inventory.setItem(8, getBackItem());
        defineInventoryAction(8, (event -> new SoulIndex().openInventory(((Player) event.getWhoClicked()))),
                Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
    }

    @Override
    public ItemStack getBackItem() {
        ItemStack backItem = ItemStack.of(Material.ARROW);
        backItem.editMeta(meta -> {
            meta.itemName(MenuLanguageDefinition.BACK_BUTTON.getSingle());
        });
        return backItem;
    }

    private ItemStack getSoulRelease() {
        ItemStack item = ItemStack.of(Material.IRON_SWORD);
        item.editMeta(meta -> {
            meta.itemName(MenuLanguageDefinition.RELEASING_SOULS.getSingle().color(NamedTextColor.YELLOW));
            meta.lore(ItemUtils.applyDefaultLoreStyle(MenuLanguageDefinition.RELEASING_SOULS_DESCRIPTION.getLines()));

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    private ItemStack getInfusion() {
        ItemStack item = ItemUtils.createCustomHead("http://textures.minecraft.net/texture/54e5a2321e639fdc9d42434aff3d7c674b4a88b2e45ed9f03723befecc9a3e7c");
        item.editMeta(meta -> {
            meta.customName(MenuLanguageDefinition.INFUSION.getSingle().color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(ItemUtils.applyDefaultLoreStyle(MenuLanguageDefinition.INFUSION_DESCRIPTION.getLines()));
        });
        return item;
    }

    private ItemStack getBinding() {
        ItemStack item = SoulLanternManager.getLanternAsCustomHead();
        item.editMeta(meta -> {
            meta.customName(MenuLanguageDefinition.BINDING.getSingle().color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(ItemUtils.applyDefaultLoreStyle(MenuLanguageDefinition.BINDING_DESCRIPTION.getLines()));
        });
        return item;
    }

    private ItemStack getVial() {
        ItemStack vial = SoulVialManager.getEmptyVial();

        vial.editMeta(meta -> {
            meta.itemName(MenuLanguageDefinition.VIAL.getSingle().color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            var lore = meta.lore();
            lore.addAll(ItemUtils.applyDefaultLoreStyle(MenuLanguageDefinition.VIAL_DESCRIPTION.getLines()));
            meta.lore(lore);
        });

        return vial;
    }

}
