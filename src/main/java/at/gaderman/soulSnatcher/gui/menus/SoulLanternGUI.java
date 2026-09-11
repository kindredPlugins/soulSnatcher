package at.gaderman.soulSnatcher.gui.menus;

import at.gaderman.soulSnatcher.config.GeneralConfig;
import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.effects.SoulEffects;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SoulLanternGUI extends ActionInventory {

    private final Player player;
    private final List<SoulInstance<?>> carriedSouls;

    public SoulLanternGUI(Player player) {
        super(MenuLanguageDefinition.SOUL_LANTERN_TITLE.getSingle().color(NamedTextColor.BLUE));

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

        int maxSouls = GeneralConfig.getInstance().MAX_BOUND_SOULS.cached();
        int spacing = 9 / maxSouls;
        int startIndex = 13 - (spacing * (maxSouls - 1) / 2);

        for (int i = 0; i < maxSouls; i++) {
            int soulSlot = startIndex + i * spacing;

            if (i < carriedSouls.size()) {
                SoulInstance<?> soul = carriedSouls.get(i);

                inventory.setItem(soulSlot, soul.soulType().itemRepresentation());
                inventory.setItem(soulSlot + 9, getRemoveItem(soul));
                defineInventoryAction(soulSlot + 9, event -> remove(soul));
            } else {
                inventory.setItem(soulSlot, getNoSoulItem());
            }
        }
    }

    private void remove(SoulInstance<?> replaced) {
        replaced.soulType().removeSoul(player);
        SoulEffects.discardSoulRewardEffect(player.getLocation().add(0, 1, 0));

        inventory.close();
    }

    private ItemStack getRemoveItem(SoulInstance<?> soul) {
        ItemStack item = ItemStack.of(Material.BARRIER);
        item.editMeta(meta -> {
                    meta.itemName(MenuLanguageDefinition.REMOVE_SOUL.getSingle().color(NamedTextColor.DARK_RED)
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral(MenuLanguageDefinition.SOUL_PLACEHOLDER)
                                    .replacement(soul.soulType().displayName())
                                    .build()));
                    meta.lore(ItemUtils.applyDefaultLoreStyle(MenuLanguageDefinition.REMOVE_SOUL_DESC.getLines()));
                }
        );
        return item;
    }

    private ItemStack getNoSoulItem() {
        ItemStack item = ItemStack.of(Material.GRAY_DYE);
        item.editMeta(meta -> {
            meta.itemName(MenuLanguageDefinition.NO_SOUL_SLOT.getSingle().color(NamedTextColor.GRAY));
            meta.lore(ItemUtils.applyDefaultLoreStyle(MenuLanguageDefinition.NO_SOUL_SLOT_DESCRIPTION.getLines()));
        });
        return item;
    }
}
