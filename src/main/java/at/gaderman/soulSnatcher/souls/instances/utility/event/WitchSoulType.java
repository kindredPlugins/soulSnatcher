package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.custom.WitchDrinkGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.souls.triggers.interact.OnConsumeItemTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@AutoService(SoulType.class)
public class WitchSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<WitchSoulType> create(LivingEntity carrier) {
        return new WitchSoulInstance(carrier, this);
    }


    @Override
    public @NotNull String id() {
        return "witch_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.WITCH;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "baf818b04d6ae98a69ad8d9463d8d0379921d3eb79222e1c6272f65372d80775";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Witch Soul", TextColor.color(0x995e9d));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When drinking potions they go on cooldown"),
                Component.text("instead of being consumed."),
                Component.text("Gain ")
                        .append(Component.text((1 - magicDamageMultiplier.cached()) * 100 + "% Magic Damage Reduction", NamedTextColor.DARK_PURPLE))
                        .append(Component.text(".", NamedTextColor.WHITE))
        );
    }

    //region

    private static final String MAGIC_DAMAGE_MULTIPLIER_CONFIG_ID = "magic_damage_multiplier";
    private final ConfigOption<Double> magicDamageMultiplier = configOption(MAGIC_DAMAGE_MULTIPLIER_CONFIG_ID, 0.4, FileConfiguration::getDouble, value -> Math.min(Math.max(value, 0), 1));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(MAGIC_DAMAGE_MULTIPLIER_CONFIG_ID, "A multiplier to how much magic damage is actually taken in percent (0.4 -> 40% of magic damage taken)");
    }

    //endregion

    public static class WitchSoulInstance extends SoulInstance<WitchSoulType> implements OnConsumeItemTrigger, OnDamageReceivedTrigger {

        private static final Map<UUID, Map<Key, Integer>> remaining_cooldowns = new HashMap<>();

        private final Set<Key> activeCooldownKeys = new LinkedHashSet<>();

        protected WitchSoulInstance(LivingEntity carrier, WitchSoulType soulType) {
            super(carrier, soulType);

            if (carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new WitchDrinkGoal(mob));

            if(carrier instanceof Player player){
                if(remaining_cooldowns.containsKey(player.getUniqueId())){
                    remaining_cooldowns.get(player.getUniqueId()).forEach((key, cooldown) -> {
                        player.setCooldown(key, cooldown);
                        activeCooldownKeys.add(key);
                    });
                    remaining_cooldowns.remove(player.getUniqueId());
                }
            }
        }

        @Override
        public void onConsumeItem(Player carrier, ItemStack item, PlayerItemConsumeEvent event) {
            if (item.getType() != Material.POTION) return;

            event.setReplacement(item);

            float cooldown = getMaxDuration(item);
            UseCooldown useCooldown = UseCooldown.useCooldown(0.0001f)
                    .cooldownGroup(createKeyForPotion(item))
                    .build();

            item.setData(DataComponentTypes.USE_COOLDOWN, useCooldown);

            activeCooldownKeys.add(useCooldown.cooldownGroup().key());

            carrier.getScheduler().run(SoulSnatcher.getPlugin(), _ -> {
                carrier.setCooldown(useCooldown.cooldownGroup().key(), (int) (cooldown * 20));
            }, null);
            carrier.setCooldown(useCooldown.cooldownGroup().key(), (int) (cooldown * 20));

            carrier.getWorld().spawnParticle(Particle.WITCH, carrier.getEyeLocation(), 20);
            carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1f, 1f);
        }

        // only creative clone can mess this up now, otherwise it's just a component for managing cooldown with no visible cooldown
        private Key createKeyForPotion(ItemStack potion) {
            UseCooldown useCooldown = potion.getData(DataComponentTypes.USE_COOLDOWN);
            return useCooldown != null ? useCooldown.cooldownGroup().key() : new NamespacedKey(SoulSnatcher.getPlugin(), UUID.randomUUID().toString());
        }

        private float getMaxDuration(ItemStack potion) {
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            return meta.getAllEffects().stream()
                    .mapToInt(effect -> effect.getType().isInstant() ? 200 : effect.getDuration())
                    .max().orElse(0) / 20.0f;
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if (event.getDamageSource().getDamageType() != DamageType.MAGIC) return;
            event.setDamage(event.getDamage() * soulType().magicDamageMultiplier.cached());
            carrier.getWorld().spawnParticle(Particle.WITCH, carrier.getEyeLocation(), 10);
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
        }

        @Override
        protected void cleanUp() {
            if(carrier() instanceof Player player) {
                Map<Key, Integer> remainingCooldowns = remaining_cooldowns.getOrDefault(player.getUniqueId(), new HashMap<>());

                activeCooldownKeys.forEach(key -> {
                    int cooldown = player.getCooldown(key);
                    if(cooldown > 0)
                        remainingCooldowns.put(key, cooldown);
                });

                remaining_cooldowns.put(player.getUniqueId(), remainingCooldowns);
            }

            super.cleanUp();
        }

        @Override
        protected void reset() {
            super.reset();

            if (carrier() instanceof Player player) {
                remaining_cooldowns.remove(player.getUniqueId());
                activeCooldownKeys.forEach(key -> player.setCooldown(key, 0));
            }
        }
    }
}
