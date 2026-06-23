package at.gaderman.soulSnatcher.gui.interaction;

import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.souls.SoulEffects;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SoulAbsorptionUI extends ActionInventory {

    private final Player player;
    private final SoulType rewardSoul;
    private final Interaction rewardTrigger;

    public SoulAbsorptionUI(Player player, SoulType rewardSoul, Interaction rewardTrigger) {
        super(Component.text("Choose a soul"));

        this.player = player;
        this.rewardSoul = rewardSoul;
        this.rewardTrigger = rewardTrigger;
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

        int rewardIndex = 4;
        inventory.setItem(rewardIndex, rewardSoul.itemRepresentation());
        inventory.setItem(rewardIndex + 9, getDiscardItem());
        defineInventoryAction(rewardIndex + 9, event -> discard());

        List<SoulInstance<?>> activeSouls = SoulType.getCarriedSouls(player);
        int startIndex = 11;
        for (int i = 0; i < activeSouls.size(); i++) {
            SoulInstance<?> soul = activeSouls.get(i);
            int soulSlot = startIndex + (i * 4);

            inventory.setItem(soulSlot, soul.soulType().itemRepresentation());
            inventory.setItem(soulSlot + 9, getOfferItem(soul));
            defineInventoryAction(soulSlot + 9, event -> replace(soul));
        }
    }

    private void discard() {
        SoulType.removeSoulReward(rewardTrigger);
        SoulEffects.discardSoulRewardEffect(rewardTrigger.getLocation());

        inventory.close();
    }

    private void replace(SoulInstance<?> replaced){
        SoulType.removeSoulReward(rewardTrigger);

        replaced.soulType().removeSoul(player);
        rewardSoul.bindSoul(player);
        SoulEffects.playBindEffect(player, rewardSoul, rewardTrigger.getLocation());

        rewardTrigger.getWorld().playSound(rewardTrigger.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 0.1f);

        inventory.close();
    }

    private ItemStack getDiscardItem() {
        ItemStack item = ItemStack.of(Material.BARRIER);
        item.editMeta(meta -> {
                    meta.itemName(Component.text("Discard ", NamedTextColor.RED)
                            .append(rewardSoul.displayName()));
                }
        );
        return item;
    }

    private ItemStack getOfferItem(SoulInstance<?> soul) {
        ItemStack item = ItemStack.of(Material.ORANGE_DYE);
        item.editMeta(meta -> {
                    meta.itemName(Component.text("Replace ", TextColor.color(0xd38531))
                            .append(soul.soulType().displayName()));
                }
        );
        return item;
    }
}
