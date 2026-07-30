package at.gaderman.soulSnatcher.souls.items;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.effects.SoulReward;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SoulVialManager implements Listener {

    private static final NamespacedKey VIAL_KEY = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_vial");
    private static final String EMPTY_VIAL = "empty";

    public SoulVialManager() {
        ShapedRecipe soulVialRecipe = new ShapedRecipe(VIAL_KEY, getEmptyVial());
        soulVialRecipe.shape("OTO", "GSG", " G ");

        soulVialRecipe.setIngredient('O', Material.SOUL_TORCH);
        soulVialRecipe.setIngredient('T', Material.GHAST_TEAR);
        soulVialRecipe.setIngredient('G', Material.GLASS);
        soulVialRecipe.setIngredient('S', Material.SOUL_SAND);

        soulVialRecipe.setCategory(CraftingBookCategory.MISC);
        soulVialRecipe.setGroup("SoulSnatcher");

        Bukkit.addRecipe(soulVialRecipe, true);
    }

    public static ItemStack getEmptyVial() {
        ItemStack vial = ItemUtils.createCustomHead("http://textures.minecraft.net/texture/" +
                "75d3a90f471c95fcc9702f6fe573cc113cdf6d8c539b261ee3c30771b18e2ac");
        vial.editMeta(meta -> {
            meta.customName(Component.text("Empty Soul Vial", TextColor.color(0x0092ff)).decoration(TextDecoration.ITALIC, false));
            var lore = ItemUtils.applyDefaultLoreStyle(
                    Component.text("Interact with an released soul to capture it.", NamedTextColor.GRAY),
                    Component.text("Can later be released again.", NamedTextColor.GRAY)
            );
            meta.lore(lore);
            meta.setMaxStackSize(16);

            meta.getPersistentDataContainer().set(VIAL_KEY, PersistentDataType.STRING, EMPTY_VIAL);
        });
        return vial;
    }

    public static ItemStack getFilledVial(SoulType soulType) {
        ItemStack vial = soulType.getRepresentativeSkull();
        vial.editMeta(meta -> {
            meta.customName(Component.text("Soul Vial ", TextColor.color(0x0092ff))
                    .append(Component.text("✦ ", NamedTextColor.GRAY))
                    .append(soulType.displayName())
                    .decoration(TextDecoration.ITALIC, false));

            var lore = ItemUtils.applyDefaultLoreStyle(
                    Component.text("Interact to release the stored soul"),
                    Component.empty(),
                    soulType.displayName()
            );
            lore.addAll(soulType.description());
            meta.lore(lore);

            meta.setMaxStackSize(1);
            meta.getPersistentDataContainer().set(VIAL_KEY, PersistentDataType.STRING, soulType.id());
        });
        vial.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(60)
                .cooldownGroup(VIAL_KEY)
                .build());
        return vial;
    }

    public static boolean checkAndFillVialIfPresent(Player player, SoulType soulType, Location soulLocation) {
        EntityEquipment equipment = player.getEquipment();

        ItemStack hand = equipment.getItemInMainHand();
        if (!EMPTY_VIAL.equals(hand.getPersistentDataContainer().getOrDefault(VIAL_KEY, PersistentDataType.STRING, "")))
            return false;

        equipment.setItemInMainHand(hand.subtract());
        ItemStack filledVial = getFilledVial(soulType);

        if(hand.isEmpty()){
            equipment.setItemInMainHand(filledVial);
        } else if(player.getInventory().firstEmpty() == -1){
            player.getWorld().dropItem(player.getLocation(), filledVial, drop -> {
                drop.setGlowing(true);
                drop.setOwner(player.getUniqueId());
                drop.setHealth(100);
                drop.setVelocity(drop.getVelocity().multiply(0));
            });
            player.sendMessage(Component.text("Your captured ", NamedTextColor.RED)
                    .append(soulType.displayName())
                    .append(Component.text(" has been dropped!", NamedTextColor.RED)));
        }else{
            player.give(filledVial);
        }

        player.getWorld().playSound(player, Sound.ENTITY_WITHER_AMBIENT, 2f, 0.5f);
        player.getWorld().playSound(player, Sound.BLOCK_ENDER_CHEST_CLOSE, 2f, 0.5f);

        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, soulLocation,
                200, 0.3, 0.3, 0.3, 0.1);
        player.getWorld().spawnParticle(Particle.SOUL, soulLocation,
                50, 0.3, 0.3, 0.3, 0.1);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, soulLocation, 30);
        return true;
    }

    @EventHandler
    public void onPlaceLantern(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getPersistentDataContainer().has(VIAL_KEY))
            event.setCancelled(true);
    }

    @EventHandler
    public void onLanternInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        if (!item.getPersistentDataContainer().has(VIAL_KEY)) return;

        Player player = event.getPlayer();

        SoulRegistry soulRegistry = SoulRegistry.getInstance();
        String soulId = item.getPersistentDataContainer().get(VIAL_KEY, PersistentDataType.STRING);
        SoulType soul = soulRegistry.getSoul(soulId);
        if(soul == null){
            SoulType legacySoul = soulRegistry.legacySoulRegistryMap().getOrDefault(soulId, null);
            if(legacySoul == null)
                return;

            event.setCancelled(true);
            player.sendActionBar(Component.text("This soul has been disabled by an admin", NamedTextColor.RED));
            player.playSound(player, Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            return;
        }

        event.setCancelled(true);

        if(player.getCooldown(VIAL_KEY) > 0) return;

        if(SoulType.getCarriedSouls(player).stream()
                .anyMatch(carried -> !carried.soulType().canOverwriteItself() && carried.soulType().equals(soul))){
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.2f);
            player.sendActionBar(Component.text("You already have bound this soul", NamedTextColor.RED));
            return;
        }

        player.getEquipment().setItemInMainHand(ItemStack.empty());
        player.setCooldown(VIAL_KEY, 60 * 20);

        Location rewardLocation = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(1));
        SoulReward.offerSoulReward(rewardLocation, player, soul);

        player.getWorld().spawnParticle(Particle.SOUL, rewardLocation, 30, 0.4, 0.4, 0.4, 0.1);
        player.getWorld().spawnParticle(Particle.EXPLOSION, rewardLocation, 1);
        player.getWorld().playSound(player, Sound.BLOCK_GLASS_BREAK, 3f, 0.5f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiglinBarter(PiglinBarterEvent event){
        if(Math.random() >= 0.05) return;

        List<ItemStack> barterDrop = event.getOutcome();
        barterDrop.clear();
        ItemStack vial = getEmptyVial();
        vial.setAmount((int) (Math.random() * 2) + 1);
        barterDrop.add(vial);
    }
}
