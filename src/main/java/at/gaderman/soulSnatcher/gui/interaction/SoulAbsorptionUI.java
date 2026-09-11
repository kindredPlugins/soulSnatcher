package at.gaderman.soulSnatcher.gui.interaction;

import at.gaderman.soulSnatcher.gui.ActionInventory;
import at.gaderman.soulSnatcher.gui.menus.MenuLanguageDefinition;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.effects.SoulEffects;
import at.gaderman.soulSnatcher.souls.effects.SoulReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
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
        super(MenuLanguageDefinition.CHOOSE_SOUL.getSingle());

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
        defineInventoryAction(rewardIndex + 9, _ -> discard());

        List<SoulInstance<?>> activeSouls = SoulType.getCarriedSouls(player);
        int startIndex = 11;

        for (int i = 0; i < activeSouls.size(); i++) {
            SoulInstance<?> soul = activeSouls.get(i);
            int distance = i / 2;
            int soulSlot = i % 2 == 0
                    ? startIndex - distance
                    : startIndex + 4 + distance;

            inventory.setItem(soulSlot, soul.soulType().itemRepresentation());
            inventory.setItem(soulSlot + 9, getOfferItem(soul));
            defineInventoryAction(soulSlot + 9, _ -> replace(soul));
        }
    }

    private void discard() {
        SoulReward.removeSoulReward(rewardTrigger);
        SoulEffects.discardSoulRewardEffect(rewardTrigger.getLocation());

        inventory.close();
    }

    private void replace(SoulInstance<?> replaced) {
        SoulReward.removeSoulReward(rewardTrigger);

        replaced.soulType().removeSoul(player);
        rewardSoul.bindSoul(player);
        SoulEffects.playBindEffect(player, rewardSoul, rewardTrigger.getLocation());

        rewardTrigger.getWorld().playSound(rewardTrigger.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 0.1f);

        inventory.close();
    }

    private ItemStack getDiscardItem() {
        ItemStack item = ItemStack.of(Material.BARRIER);
        item.editMeta(meta -> {
                    meta.itemName(MenuLanguageDefinition.DISCARD_SOUL.getSingle().color(NamedTextColor.RED)
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral(MenuLanguageDefinition.SOUL_PLACEHOLDER)
                                    .replacement(rewardSoul.displayName())
                                    .build()));
                }
        );
        return item;
    }

    private ItemStack getOfferItem(SoulInstance<?> soul) {
        ItemStack item = ItemStack.of(Material.ORANGE_DYE);
        item.editMeta(meta -> {
                    meta.itemName(MenuLanguageDefinition.REPLACE_SOUL.getSingle().replaceText(TextReplacementConfig.builder()
                            .matchLiteral(MenuLanguageDefinition.SOUL_PLACEHOLDER)
                            .replacement(soul.soulType().displayName())
                            .build()));
                }
        );
        return item;
    }
}
