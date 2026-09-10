package at.gaderman.soulSnatcher.souls.instances;

import at.gaderman.soulSnatcher.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum SoulCategory {
    COMBAT,
    UTILITY,
    MOVEMENT,
    ATTRIBUTES;

    public ItemStack icon() {
        return switch (this) {
            case COMBAT -> ItemUtils.createBasicUIItem(Material.IRON_SWORD, SoulCategoryLanguageDefinitions.COMBAT_TITLE.getSingle().color(NamedTextColor.AQUA),
                    ItemUtils.applyDefaultLoreStyle(SoulCategoryLanguageDefinitions.COMBAT_DESCRIPTION.getLines()));
            case UTILITY -> ItemUtils.createBasicUIItem(Material.CHEST, SoulCategoryLanguageDefinitions.UTILITY_TITLE.getSingle().color(NamedTextColor.YELLOW),
                    ItemUtils.applyDefaultLoreStyle(SoulCategoryLanguageDefinitions.UTILITY_DESCRIPTION.getLines()));
            case MOVEMENT -> ItemUtils.createBasicUIItem(Material.FEATHER, SoulCategoryLanguageDefinitions.MOVEMENT_TITLE.getSingle().color(NamedTextColor.GREEN),
                    ItemUtils.applyDefaultLoreStyle(SoulCategoryLanguageDefinitions.MOVEMENT_DESCRIPTION.getLines()));
            case ATTRIBUTES -> ItemUtils.createBasicUIItem(Material.ANVIL, SoulCategoryLanguageDefinitions.ATTRIBUTES_TITLE.getSingle(),
                    ItemUtils.applyDefaultLoreStyle(SoulCategoryLanguageDefinitions.ATTRIBUTES_DESCRIPTION.getLines()));
        };
    }
}
