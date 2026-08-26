package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.SkeletonShootGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.action.OnItemDamageTrigger;
import at.gaderman.soulSnatcher.souls.triggers.interact.OnStopUsingItemTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityShootBowTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class SkeletonSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance<SkeletonSoulType> create(LivingEntity carrier) {
        return new SkeletonSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "skeleton_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SKELETON;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "482b78da6ee713d5acfe5fcb0754ee56900831a5098313064108de6e7e406839";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Skeleton Soul", NamedTextColor.GRAY);
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Shoot ")
                        .append(Component.text("infinite ", NamedTextColor.GREEN))
                        .append(Component.text("arrows of any kind")),
                Component.text("while using no durability of bows.")
        );
    }

    public static class SkeletonSoulInstance extends SoulInstance<SkeletonSoulType> implements OnStopUsingItemTrigger, OnItemDamageTrigger, OnEntityShootBowTrigger {
        protected SkeletonSoulInstance(LivingEntity carrier, SkeletonSoulType soulType) {
            super(carrier, soulType);

            if(isInfused())
                Bukkit.getMobGoals().addGoal((Mob) carrier, 0, new SkeletonShootGoal((Mob) carrier, 5000));
        }

        private int arrowIndex;

        @Override
        public void onStopUsingItem(Player player, PlayerStopUsingItemEvent event) {
            ItemStack bow = event.getItem();
            if(bow.getType() != Material.BOW) return;

            PlayerInventory inv = player.getInventory();

            int slot = -1;
            if (isArrow(inv.getItemInOffHand())) {
                slot = 40; // offhand slot index
            } else {
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && isArrow(item)) { slot = i; break; }
                }
            }

            arrowIndex = slot;
        }

        private boolean isArrow(ItemStack item){
            Material mat = item.getType();
            return mat == Material.ARROW || mat == Material.SPECTRAL_ARROW || mat == Material.TIPPED_ARROW;
        }

        @Override
        public void onEntityShootBow(LivingEntity carrier, EntityShootBowEvent event) {
            if(!(carrier instanceof Player player)) return;

            ItemStack consumed = event.getConsumable();
            if(consumed == null) return;

            PlayerInventory inv = player.getInventory();

            if(!player.getGameMode().isInvulnerable()) {
                Bukkit.getScheduler().runTask(SoulSnatcher.getPlugin(), () -> {
                    ItemStack itemAtIndex = inv.getItem(arrowIndex);
                    if(inv.getItemInMainHand().getEnchantmentLevel(Enchantment.INFINITY) > 0 && consumed.getType() == Material.ARROW)
                        return;

                    if (itemAtIndex == null || itemAtIndex.getType().isAir())
                        inv.setItem(arrowIndex, consumed);

                    else inv.setItem(arrowIndex, itemAtIndex.add(1));
                });
                SoulSnatcher.getPlugin().registerDelayedTask(() -> {

                }, 1L);
            }

            if(event.getProjectile() instanceof AbstractArrow arrow)
                arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
        }

        @Override
        public void onItemDamage(Player player, PlayerItemDamageEvent event) {
            if(event.getItem().getType() != Material.BOW) return;

            event.setCancelled(true);
        }
    }
}
