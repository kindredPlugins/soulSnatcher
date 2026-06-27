package at.gaderman.soulSnatcher.gui.menus;

import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.souls.items.SoulLanternManager;
import at.gaderman.soulSnatcher.souls.items.SoulVialManager;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class GamePlayInfoGUI extends ActionInventory {

    public GamePlayInfoGUI() {
        super(27, Component.text("GameplayInfo", NamedTextColor.GOLD));
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

    private ItemStack getSoulRelease() {
        ItemStack item = ItemStack.of(Material.IRON_SWORD);
        item.editMeta(meta -> {
            meta.itemName(Component.text("Releasing Souls", NamedTextColor.YELLOW));
            meta.lore(ItemUtils.applyDefaultLoreStyle(
                    Component.text("After slaying a mob a ")
                            .append(Component.text("soul ", NamedTextColor.BLUE)),
                    Component.text("is released and added to your pool.")
            ));

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    private ItemStack getInfusion() {
        ItemStack item = ItemUtils.createCustomHead("http://textures.minecraft.net/texture/54e5a2321e639fdc9d42434aff3d7c674b4a88b2e45ed9f03723befecc9a3e7c");
        item.editMeta(meta -> {
            meta.customName(Component.text("Infusion", TextColor.color(0x20a0ad)).decoration(TextDecoration.ITALIC, false));
            meta.lore(ItemUtils.applyDefaultLoreStyle(
                    Component.text("Nearby spawned mobs take a ")
                            .append(Component.text("soul ", NamedTextColor.BLUE)),
                    Component.text("from your pool and ")
                            .append(Component.text("infuse", TextColor.color(0x20a0ad)))
                            .append(Component.text(".", NamedTextColor.WHITE)),
                    Component.text("They gain additional mechanics based"),
                    Component.text("on the ")
                            .append(Component.text("soul ", NamedTextColor.BLUE))
                            .append(Component.text("they infused with.", NamedTextColor.WHITE))
            ));
        });
        return item;
    }

    private ItemStack getBinding() {
        ItemStack item = SoulLanternManager.getLanternAsCustomHead();
        item.editMeta(meta -> {
            meta.customName(Component.text("Binding", TextColor.color(0x20a0ad)).decoration(TextDecoration.ITALIC, false));
            meta.lore(ItemUtils.applyDefaultLoreStyle(
                    Component.text("After killing an ")
                            .append(Component.text("infused ", TextColor.color(0x20a0ad)))
                            .append(Component.text("mob", NamedTextColor.WHITE)),
                    Component.text("their infused ")
                            .append(Component.text("soul ", NamedTextColor.BLUE))
                            .append(Component.text("will be offered.", NamedTextColor.WHITE)),
                    Component.text("Such ")
                            .append(Component.text("soul ", NamedTextColor.BLUE))
                            .append(Component.text("can be absorbed to gain", NamedTextColor.WHITE)),
                    Component.text("additional mechanics for yourself.")
            ));
        });
        return item;
    }

    private ItemStack getVial() {
        ItemStack vial = SoulVialManager.getEmptyVial();

        vial.editMeta(meta -> {
            var lore = meta.lore();
            lore.addAll(ItemUtils.applyDefaultLoreStyle(
                    Component.empty(),
                    Component.text("Can be obtained through ")
                            .append(Component.text("Piglin Bartering", NamedTextColor.GOLD))
                            .append(Component.text(".", NamedTextColor.WHITE))
            ));
            meta.lore(lore);
        });

        return vial;
    }

}
