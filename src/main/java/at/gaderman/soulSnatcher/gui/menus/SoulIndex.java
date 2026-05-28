package at.gaderman.soulSnatcher.gui.menus;

import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SoulIndex extends ActionInventory {

    public SoulIndex(){
        super(Component.text("SoulIndex", NamedTextColor.BLUE));
    }

    private List<SoulType> soulTypes = new ArrayList<>();
    private List<SoulType> filteredSoulTypes = new ArrayList<>();

    @Override
    protected int calculateSize() {
        this.soulTypes = SoulRegistry.getInstance().soulRegistryMap().values().stream()
                .sorted(Comparator.comparing(soultype -> PlainTextComponentSerializer.plainText().serialize(soultype.displayName())))
                .toList();
        return (Math.ceilDiv(soulTypes.size(), 9) + 2) * 9;
    }

    @Override
    protected void initializeInventory(int size, Component inventoryName) {
        super.initializeInventory(size, inventoryName);

        for(int i = 0; i < 9; i++)
            inventory.setItem(i, getFillItem());

        for(int i = size - 9; i < size; i++)
            inventory.setItem(i, getFillItem());

        for(int i = 9; i < size - 9; i++)
            inventory.setItem(i, getFillItem().withType(Material.LIGHT_GRAY_STAINED_GLASS_PANE));

        for(int i = 0; i < soulTypes.size(); i++){
            int index = i + 9;
            SoulType soulType = this.soulTypes.get(i);

            inventory.setItem(index, soulType.itemRepresentation());
        }

        inventory.setItem(size - 5, getFilterItem());
        //defineInventoryAction(size - 5, (event) -> ((Player) event.getWhoClicked()).openSign());
    }

    private ItemStack getFilterItem(){
        ItemStack item = ItemStack.of(Material.OAK_SIGN);
        item.editMeta(meta -> {
            meta.itemName(Component.text("Filter"));
        });
        return item;
    }
}
