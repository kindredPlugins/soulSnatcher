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
            case COMBAT -> ItemUtils.createBasicUIItem(Material.IRON_SWORD, Component.text("Combat", NamedTextColor.AQUA),
                    ItemUtils.applyDefaultLoreStyle(
                       Component.text("Souls which directly aid combat prowess")
                    ));
            case UTILITY -> ItemUtils.createBasicUIItem(Material.CHEST, Component.text("Utility", NamedTextColor.YELLOW),
                    ItemUtils.applyDefaultLoreStyle(
                            Component.text("Souls which have vastly affect gameplay")
                    ));
            case MOVEMENT -> ItemUtils.createBasicUIItem(Material.FEATHER, Component.text("Movement", NamedTextColor.GREEN),
                    ItemUtils.applyDefaultLoreStyle(
                            Component.text("Souls which boost your movement capabilities")
                    ));
            case ATTRIBUTES -> ItemUtils.createBasicUIItem(Material.ANVIL, Component.text("Attributes", NamedTextColor.BLUE),
                    ItemUtils.applyDefaultLoreStyle(
                            Component.text("Souls which change attributes of yourself")
                    ));
        };
    }
}
