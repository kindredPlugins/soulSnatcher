package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.OnEntityEquipmentTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnTargetTrigger;
import com.google.auto.service.AutoService;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@AutoService(SoulType.class)
public class PiglinSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance<PiglinSoulType> create(LivingEntity carrier) {
        return new PiglinSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "piglin_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.PIGLIN;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.ATTRIBUTES;
    }

    @Override
    public boolean isInvalidInfusionTarget(LivingEntity entity) {
        return super.isInvalidInfusionTarget(entity) || (!(entity instanceof Zombie) && !(entity instanceof Skeleton));
    }

    @Override
    protected @NotNull String skullTexture() {
        return "a792b6997d739f535beed3ab1d4aeadfa76777bf8e38a666f54f82ff9f858186";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Piglin Soul", TextColor.color(0xf2bb87));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }

    public static class PiglinSoulInstance extends SoulInstance<PiglinSoulType> implements OnEntityEquipmentTrigger, OnTargetTrigger {
        protected PiglinSoulInstance(LivingEntity carrier, PiglinSoulType soulType) {
            super(carrier, soulType);

            if (isInfused()) {
                EntityEquipment equipment = carrier.getEquipment();
                if (equipment == null) return;

                Arrays.stream(EquipmentSlot.values())
                        .filter(slot -> slot.isArmor() || slot == EquipmentSlot.HAND)
                        .filter(slot -> equipment.getItem(slot).isEmpty())
                        .forEach(slot -> equipment.setItem(slot,
                                Bukkit.getItemFactory().enchantWithLevels(ItemStack.of(getGoldArmorForSlot(slot)),
                                        (int) (Math.random() * 30), true, new Random()))
                        );
            }

            updateGoldCount();
        }

        private static final NamespacedKey GOLD_BONUS = new NamespacedKey(SoulSnatcher.getPlugin(), "piglin_soul_gold_bonus");

        private int previousGoldCount;
        private BukkitTask goldSparkle;

        private static final List<AbstractMap.SimpleEntry<Attribute, AttributeModifier>> GOLD_BONUS_MAP = List.of(
                createAttributeModEntry(Attribute.ATTACK_DAMAGE, +1, AttributeModifier.Operation.ADD_NUMBER),
                createAttributeModEntry(Attribute.ARMOR, +2, AttributeModifier.Operation.ADD_NUMBER),
                createAttributeModEntry(Attribute.MOVEMENT_SPEED, +0.2, AttributeModifier.Operation.ADD_SCALAR),
                createAttributeModEntry(Attribute.ARMOR_TOUGHNESS, +2, AttributeModifier.Operation.ADD_NUMBER),
                createAttributeModEntry(Attribute.ATTACK_SPEED, +0.2, AttributeModifier.Operation.ADD_NUMBER),
                createAttributeModEntry(Attribute.KNOCKBACK_RESISTANCE, +0.25, AttributeModifier.Operation.ADD_NUMBER)
        );

        private static AbstractMap.SimpleEntry<Attribute, AttributeModifier> createAttributeModEntry(Attribute attribute, double amount, AttributeModifier.Operation operation) {
            return new AbstractMap.SimpleEntry<>(attribute, new AttributeModifier(GOLD_BONUS, amount, operation));
        }

        private void updateGoldCount(){
            LivingEntity carrier = carrier();

            EntityEquipment equipment = carrier.getEquipment();
            if(equipment == null) return;

            int goldAmount = Math.toIntExact(Arrays.stream(EquipmentSlot.values())
                    .filter(slot -> slot.isArmor() || slot.isHand())
                    .filter(slot -> equipment.getItem(slot).getType().toString().contains("GOLD"))
                    .count());

            cleanUp();

            for (int i = 0; i < goldAmount; i++) {
                var goldBonusEntry = GOLD_BONUS_MAP.get(i);

                AttributeInstance attribute = carrier.getAttribute(goldBonusEntry.getKey());
                if (attribute != null)
                    attribute.addModifier(goldBonusEntry.getValue());
            }

            if(previousGoldCount < goldAmount){
                carrier.getWorld().playSound(carrier, Sound.ENTITY_PIGLIN_ADMIRING_ITEM, 1f, 1f);
                carrier.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, carrier.getEyeLocation(), 20, 0.2, 0.2, 0.2, 0.5);

                if(goldSparkle == null){
                    goldSparkle = new BukkitRunnable(){
                        @Override
                        public void run() {
                            if(previousGoldCount == 0 || !carrier.isValid()){
                                goldSparkle = null;
                                cancel();
                                return;
                            }

                            carrier.getWorld().spawnParticle(Particle.WAX_ON, carrier.getEyeLocation(), 1, 0.2, 0.5, 0.2);
                        }
                    }.runTaskTimer(SoulSnatcher.getPlugin(), 20, 20);
                }
            }

            if (carrier instanceof Player player) {
                if (previousGoldCount < goldAmount) {
                    player.sendActionBar(Component.text("⛃ Gold Power ", NamedTextColor.GOLD)
                            .append(Component.text(previousGoldCount, NamedTextColor.GRAY))
                            .append(Component.text(" -> ", NamedTextColor.GOLD))
                            .append(Component.text(goldAmount, NamedTextColor.GREEN)));
                } else if (previousGoldCount > goldAmount) {
                    player.sendActionBar(Component.text("⛃ Gold Power ", NamedTextColor.GOLD)
                            .append(Component.text(previousGoldCount, NamedTextColor.GRAY))
                            .append(Component.text(" -> ", NamedTextColor.GOLD))
                            .append(Component.text(goldAmount, NamedTextColor.RED)));
                    player.playSound(player, Sound.ENTITY_PIGLIN_HURT, 1f, 0.25f);
                }
            }

            previousGoldCount = goldAmount;
        }

        @Override
        public void onEntityEquipmentChange(LivingEntity carrier, EntityEquipmentChangedEvent event) {
            updateGoldCount();
        }

        private static Material getGoldArmorForSlot(EquipmentSlot slot) {
            return switch (slot) {
                case HEAD -> Material.GOLDEN_HELMET;
                case CHEST -> Material.GOLDEN_CHESTPLATE;
                case LEGS -> Material.GOLDEN_LEGGINGS;
                case FEET -> Material.GOLDEN_BOOTS;
                case HAND -> Material.GOLDEN_SWORD;
                default -> Material.GOLD_INGOT;
            };
        }

        @Override
        public void onBeingTargeted(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event) {}

        @Override
        public void onCarrierTarget(LivingEntity carrier, LivingEntity target, EntityTargetLivingEntityEvent event) {
            if(event.getReason() != EntityTargetEvent.TargetReason.CLOSEST_PLAYER) return;
            if(!(target instanceof Player player)) return;

            EntityEquipment equipment = player.getEquipment();
            boolean shouldIgnore = Arrays.stream(EquipmentSlot.values())
                    .filter(EquipmentSlot::isArmor)
                    .anyMatch(slot -> equipment.getItem(slot).getType().toString().contains("GOLD"));

            if(shouldIgnore)
                event.setCancelled(true);
        }

        @Override
        protected void cleanUp() {
            Registry.ATTRIBUTE.stream().forEach(attr -> {
                var carrierAttribute = carrier().getAttribute(attr);
                if (carrierAttribute != null) carrierAttribute.removeModifier(GOLD_BONUS);
            });
        }
    }
}
