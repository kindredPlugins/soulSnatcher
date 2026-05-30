package at.gaderman.soulSnatcher.gui.menus;

import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SoulIndex extends ActionInventory {

    public SoulIndex() {
        super(Component.text("SoulIndex", NamedTextColor.BLUE));
    }

    private static List<SoulCategory> categories = new ArrayList<>();

    @Override
    protected int calculateSize() {
        if (categories.isEmpty())
            categories = SoulRegistry.getInstance().soulRegistryMap().values().stream()
                    .map(SoulType::category)
                    .distinct()
                    .sorted(Comparator.comparing(category -> PlainTextComponentSerializer.plainText().serialize(category.icon().displayName())))
                    .toList();

        return (Math.ceilDiv(categories.size(), 9) + 2) * 9;
    }

    @Override
    protected void initializeInventory(int size, Component inventoryName) {
        super.initializeInventory(size, inventoryName);

        fillWithFillItem();

        int offsetPerCat = Math.max(Math.floorDiv(9, categories.size()) - 1, 0);

        for (int i = 0; i < categories.size(); i++) {
            int index = i + 9 + offsetPerCat * (i + 1);
            SoulCategory category = categories.get(i);

            inventory.setItem(index, category.icon());
            defineInventoryAction(index, event -> {
                if (event.getWhoClicked() instanceof Player player)
                    new CategorizedSoulIndex(category).openInventory(player);
            }, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
        }
    }
}
