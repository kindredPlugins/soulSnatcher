package at.gaderman.soulSnatcher.gui.menus;

import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.effects.SoulReward;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CategorizedSoulIndex extends ActionInventory {

    private final SoulCategory category;

    public CategorizedSoulIndex(SoulCategory category) {
        super(Component.text("SoulIndex", NamedTextColor.BLUE));

        this.category = category;
    }

    private List<SoulType> soulTypes = new ArrayList<>();

    @Override
    protected int calculateSize() {
        if (soulTypes.isEmpty())
            soulTypes = SoulRegistry.getInstance().soulRegistryMap().values().stream()
                    .filter(soulType -> soulType.category() == category)
                    .sorted(Comparator.comparing(soultype -> PlainTextComponentSerializer.plainText().serialize(soultype.displayName())))
                    .toList();

        return (Math.ceilDiv(soulTypes.size(), 9) + 1) * 9;
    }

    @Override
    protected void initializeInventory(int size, Component inventoryName) {
        super.initializeInventory(size, inventoryName);

        for (int i = 0; i < 9; i++)
            inventory.setItem(i, getFillItem());

        for (int i = 9; i < size; i++)
            inventory.setItem(i, getFillItem().withType(Material.LIGHT_GRAY_STAINED_GLASS_PANE));

        inventory.setItem(4, category.icon());
        inventory.setItem(8, getBackItem());
        defineInventoryAction(8, (event) -> {
            if (event.getWhoClicked() instanceof Player player)
                new SoulIndex().openInventory(player);
        }, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);

        for (int i = 0; i < soulTypes.size(); i++) {
            int index = i + 9;
            SoulType soulType = soulTypes.get(i);

            inventory.setItem(index, soulType.itemRepresentation());
            defineInventoryAction(index, (event) -> {
                if(event.getClick().isRightClick() && (event.getWhoClicked() instanceof Player player) && player.getGameMode() == GameMode.CREATIVE)
                    SoulReward.offerSoulReward(player.getLocation().add(player.getLocation().getDirection()), player, soulType);
            });
        }

    }

}
