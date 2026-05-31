package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.SkeletonShootGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.OnItemDamageTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnPlayerInteractTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnEntityShootBowTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class SkeletonSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
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
        return List.of();
    }

    public static class SkeletonSoulInstance extends SoulInstance implements OnPlayerInteractTrigger, OnItemDamageTrigger, OnEntityShootBowTrigger {
        protected SkeletonSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);

            if(isInfused())
                Bukkit.getMobGoals().addGoal((Mob) carrier, 0, new SkeletonShootGoal((Mob) carrier, 5000));
        }

        private int arrowIndex;

        @Override
        public void onPlayerInteract(Player player, PlayerInteractEvent event) {
            if(event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
            ItemStack bow = event.getItem();
            if(bow == null || bow.getType() != Material.BOW) return;

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

            SoulSnatcher.getPlugin().registerDelayedTask(() -> {
                ItemStack itemAtIndex = inv.getItem(arrowIndex);
                if(itemAtIndex == null || itemAtIndex.getType().isAir())
                    inv.setItem(arrowIndex, consumed);

                else inv.setItem(arrowIndex, itemAtIndex.add(1));
            }, 1L);

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
