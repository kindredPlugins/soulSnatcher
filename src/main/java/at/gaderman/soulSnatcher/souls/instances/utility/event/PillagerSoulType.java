package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnEntityKillTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@AutoService(SoulType.class)
public class PillagerSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new PillagerSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "pillager_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.PILLAGER;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "32fb80a6b6833e31d9ce8313a54777645f9c1e55b810918a706e7bcc8d35a5a2";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Pillager Soul", TextColor.color(0x582711));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }

    @Override
    public boolean isInvalidInfusionTarget(LivingEntity entity) {
        return super.isInvalidInfusionTarget(entity) || (!(entity instanceof Zombie) && !(entity instanceof Skeleton)
                && !(entity instanceof Piglin) && !(entity instanceof Illager));
    }

    private static final double EXTRA_LOOT_CHANCE = 0.2;

    public static class PillagerSoulInstance extends SoulInstance implements OnEntityKillTrigger {
        protected PillagerSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);

            if (isInfused()) {
                EntityEquipment equipment = carrier.getEquipment();
                if (equipment == null) return;

                Arrays.stream(EquipmentSlot.values())
                        .filter(slot -> slot.isArmor() || slot == EquipmentSlot.HAND)
                        .forEach(slot -> {
                                    ItemStack setItem = equipment.getItem(slot);

                                    if (setItem.isEmpty()) {
                                        List<Tag<Material>> itemTags = switch (slot) {
                                            case HEAD -> List.of(Tag.ITEMS_HEAD_ARMOR);
                                            case CHEST -> List.of(Tag.ITEMS_CHEST_ARMOR);
                                            case LEGS -> List.of(Tag.ITEMS_LEG_ARMOR);
                                            case FEET -> List.of(Tag.ITEMS_FOOT_ARMOR);
                                            case HAND -> List.of(Tag.ITEMS_SWORDS, Tag.ITEMS_AXES);
                                            default -> List.of();
                                        };

                                        List<Material> possibleMats = itemTags.stream()
                                                .flatMap(tag -> tag.getValues().stream())
                                                .filter(mat -> !mat.toString().contains("NETHERITE"))
                                                .toList();
                                        if (possibleMats.isEmpty()) return;

                                        setItem = ItemStack.of(possibleMats.get((int) (Math.random() * possibleMats.size())));
                                    }

                                    equipment.setItem(slot, Bukkit.getItemFactory().enchantWithLevels(setItem, 30, true, new Random(carrier.getWorld().getSeed())));
                                }
                        );
            }
        }

        @Override
        public void onEntityKillTrigger(Player player, LivingEntity killed, EntityDeathEvent event) {
            if (killed instanceof Player) return;

            List<ItemStack> drops = event.getDrops().stream()
                    .filter(drop -> drop.getMaxStackSize() != 1)
                    .toList();

            if (Math.random() >= EXTRA_LOOT_CHANCE) return;

            event.getDrops().add(drops.get((int) (Math.random() * drops.size())).asOne());

            for (int i = 0; i < 8; i++) {
                killed.getWorld().dropItem(killed.getLocation(), ItemStack.of(Material.EMERALD), item -> {
                   item.setCanMobPickup(false);
                   item.setCanPlayerPickup(false);

                   item.setTicksLived(6000 - 15);
                   item.setVelocity(new Vector(createRandomAxis(0.25), 0.225, createRandomAxis(0.25)));
                });
            }
            killed.getWorld().playSound(killed.getLocation(), Sound.ENTITY_PILLAGER_CELEBRATE, 1f, 1.3f);
        }

        private double createRandomAxis(double maxValue){
            return Math.random() * maxValue * 2 - maxValue;
        }
    }
}
