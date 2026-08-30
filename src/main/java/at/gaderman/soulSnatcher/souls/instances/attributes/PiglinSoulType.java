package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.action.OnEntityEquipmentTrigger;
import at.gaderman.soulSnatcher.souls.triggers.action.OnTargetTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@AutoService(SoulType.class)
public class PiglinSoulType extends ConfigHoldingSoulType {

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
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Gain multiple ")
                        .append(Component.text("attribute bonuses ", NamedTextColor.BLUE)),
                Component.text("per ")
                        .append(Component.text("gold piece ", NamedTextColor.GOLD))
                        .append(Component.text("worn.", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static String GOLD_BONUS_CONFIG_ID = "gold_bonus";

    private static final List<GoldBonusEntry> DEFAULT_GOLD_BONUS_LIST = List.of(
            new GoldBonusEntry(Attribute.ATTACK_DAMAGE, +1, AttributeModifier.Operation.ADD_NUMBER),
            new GoldBonusEntry(Attribute.ARMOR, +2, AttributeModifier.Operation.ADD_NUMBER),
            new GoldBonusEntry(Attribute.MOVEMENT_SPEED, +0.2, AttributeModifier.Operation.ADD_SCALAR),
            new GoldBonusEntry(Attribute.ARMOR_TOUGHNESS, +2, AttributeModifier.Operation.ADD_NUMBER),
            new GoldBonusEntry(Attribute.ATTACK_SPEED, +0.2, AttributeModifier.Operation.ADD_NUMBER),
            new GoldBonusEntry(Attribute.KNOCKBACK_RESISTANCE, +0.25, AttributeModifier.Operation.ADD_NUMBER)
    );

    private final ConfigOption<List<GoldBonusEntry>> goldBonusMap = configOption(
            GOLD_BONUS_CONFIG_ID,
            DEFAULT_GOLD_BONUS_LIST,
            (config, path, def) -> {
                ConfigurationSection section = config.getConfigurationSection(path);
                if (section == null) return def;

                List<GoldBonusEntry> entries = new ArrayList<>();
                List<String> keys = new ArrayList<>(section.getKeys(false));
                keys.sort(Comparator.comparingInt(k -> {
                    try { return Integer.parseInt(k); }
                    catch (NumberFormatException e) { return Integer.MAX_VALUE; }
                }));

                for (String key : keys) {
                    ConfigurationSection entry = section.getConfigurationSection(key);
                    if (entry == null) continue;

                    try {
                        Attribute attr = Registry.ATTRIBUTE.get(
                                NamespacedKey.minecraft(entry.getString("attribute", "").toLowerCase()));
                        double value = entry.getDouble("value");
                        AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(
                                entry.getString("operation", "ADD_NUMBER").toUpperCase());

                        if (attr != null) entries.add(new GoldBonusEntry(attr, value, op));
                    } catch (IllegalArgumentException e) {
                        SoulSnatcher.getPlugin().getLogger().warning(
                                "Invalid gold_bonus entry '" + key + "' in piglin_soul config, skipping");
                    }
                }
                return entries.isEmpty() ? def : entries;
            }
    );

    @Override
    public Map<String, Object> extraConfigPathValueMap() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(GOLD_BONUS_CONFIG_ID, "List of attribute buffs per gold piece worn, attribute is a direct key to minecraft attribute, operation is either ADD_NUMBER, ADD_SCALAR or MULTIPLY_SCALAR_1");
    }

    @Override
    public void writeExtraConfigDefaults(YamlConfiguration config, String basePath) {
        String sectionPath = basePath + "." + GOLD_BONUS_CONFIG_ID;
        if (config.contains(sectionPath)) return;

        for (int i = 0; i < DEFAULT_GOLD_BONUS_LIST.size(); i++) {
            GoldBonusEntry entry = DEFAULT_GOLD_BONUS_LIST.get(i);
            String entryPath = sectionPath + "." + i;
            config.set(entryPath + ".attribute", entry.attribute().key().value());
            config.set(entryPath + ".value", entry.value());
            config.set(entryPath + ".operation", entry.operation().name());
        }
    }

    //endregion

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

        private static final String GOLD_BONUS = "piglin_soul_gold_bonus_";

        private int previousGoldCount;
        private BukkitTask goldSparkle;

        private void updateGoldCount() {
            LivingEntity carrier = carrier();

            EntityEquipment equipment = carrier.getEquipment();
            if (equipment == null) return;

            int goldAmount = Math.toIntExact(Arrays.stream(EquipmentSlot.values())
                    .filter(slot -> slot.isArmor() || slot.isHand())
                    .filter(slot -> equipment.getItem(slot).getType().toString().contains("GOLD"))
                    .count());

            cleanUp();

            for (int i = 0; i < goldAmount; i++) {
                var goldBonusEntry = soulType().goldBonusMap.cached().get(i);

                AttributeInstance attribute = carrier.getAttribute(goldBonusEntry.attribute);
                if (attribute != null)
                    attribute.addModifier(new AttributeModifier(new NamespacedKey(SoulSnatcher.getPlugin(), GOLD_BONUS + i), goldBonusEntry.value, goldBonusEntry.operation));
            }

            if (previousGoldCount < goldAmount) {
                carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_PIGLIN_ADMIRING_ITEM, 1f, 1f);
                carrier.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, carrier.getEyeLocation(), 20, 0.2, 0.2, 0.2, 0.5);

                if (goldSparkle == null) {
                    goldSparkle = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (previousGoldCount == 0 || !carrier.isValid()) {
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
        public void onBeingTargeted(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event) {
        }

        @Override
        public void onCarrierTarget(LivingEntity carrier, LivingEntity target, EntityTargetLivingEntityEvent event) {
            if (event.getReason() != EntityTargetEvent.TargetReason.CLOSEST_PLAYER) return;
            if (!(target instanceof Player player)) return;

            EntityEquipment equipment = player.getEquipment();
            boolean shouldIgnore = Arrays.stream(EquipmentSlot.values())
                    .filter(EquipmentSlot::isArmor)
                    .anyMatch(slot -> equipment.getItem(slot).getType().toString().contains("GOLD"));

            if (shouldIgnore)
                event.setCancelled(true);
        }

        @Override
        protected void cleanUp() {
            Registry.ATTRIBUTE.stream().forEach(attr -> {
                var carrierAttribute = carrier().getAttribute(attr);
                if (carrierAttribute != null) carrierAttribute.getModifiers().forEach(mod -> {
                    if(mod.getKey().value().startsWith(GOLD_BONUS))
                        carrierAttribute.removeModifier(mod);
                });
            });
        }
    }

    private record GoldBonusEntry(Attribute attribute, double value, AttributeModifier.Operation operation){}
}
