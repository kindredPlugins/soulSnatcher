package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@AutoService(SoulType.class)
public class HuskSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<HuskSoulType> create(LivingEntity carrier) {
        return new HuskSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "husk_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.HUSK;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "c096164f81950a5cc0e33e87999f98cde792517f4d7f99a647a9aedab23ae58";
    }

    @Override
    public @NotNull Component defaultDisplayName() {
        return Component.text("Husk", TextColor.color(0xc9ad70));
    }

    @Override
    public @NotNull List<Component> defaultDescription() {
        return List.of(
                Component.text("Any melee hit will steal "),
                Component.text("exhaustion ", TextColor.color(0xc9ad70))
                        .append(Component.text("from the target.")),
                Component.text("(Mobs lose portions of movement speed)", NamedTextColor.GRAY)
        );
    }

    @Override
    public boolean isInvalidInfusionTarget(LivingEntity entity) {
        return super.isInvalidInfusionTarget(entity) || entity.getAttribute(Attribute.MOVEMENT_SPEED) == null;
    }

    //region Config Values

    private static final String EXHAUSTION_AMOUNT_CONFIG_ID = "exhaustion_amount";
    private static final String INFUSED_SPEED_STACKS_CONFIG_ID = "mob_speed_stacks";

    private final ConfigOption<Double> exhaustion_amount = configOption(EXHAUSTION_AMOUNT_CONFIG_ID, 0.5, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Integer> infused_speed_stacks = configOption(INFUSED_SPEED_STACKS_CONFIG_ID, 4, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                EXHAUSTION_AMOUNT_CONFIG_ID, "How much exhaustion is stolen from the target, when the carrier has minus exhaustion they gain 1 saturation (2.75 exhaustion = around 1 hunger point loss)",
                INFUSED_SPEED_STACKS_CONFIG_ID, "Mobs do not have exhaustion or saturation, so instead they receive a 10% speed buff or debuff, this is the maximum of stack they can gain"
        );
    }

    //endregion

    public static class HuskSoulInstance extends SoulInstance<HuskSoulType> implements OnDamageDealtTrigger {
        private static final NamespacedKey MOB_EXHAUST_SPEED_KEY = new NamespacedKey(SoulSnatcher.getPlugin(), "husk_soul_mob_exhaustion_bonus");
        private static final double EXHAUST_SPEED_AMOUNT = 0.1;

        private static final Set<UUID> triggerSteal = new HashSet<>();

        protected HuskSoulInstance(LivingEntity carrier, HuskSoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if (carrier instanceof Player && !(event.getDamager() instanceof Player)) return;

            triggerSteal.add(target.getUniqueId());

            if (target instanceof Player playerTarget) {
                playerTarget.setExhaustion(playerTarget.getExhaustion() + soulType().exhaustion_amount.cached().floatValue());
            } else {
                applyExhaustBonus(target, false);
            }

            if (!triggerSteal.contains(event.getDamager().getUniqueId())) {
                if (carrier instanceof Player player) {
                    player.setExhaustion(player.getExhaustion() - soulType().exhaustion_amount.cached().floatValue());

                    if (player.getExhaustion() < 0) {
                        player.setSaturation(player.getSaturation() + 1);
                        player.playSound(player, Sound.ENTITY_GENERIC_EAT, 1, 1);
                        player.setExhaustion(soulType().exhaustion_amount.cached().floatValue() * 1.5f);
                    }
                } else {
                    applyExhaustBonus(carrier, true);
                }

                carrier.getScheduler().run(SoulSnatcher.getPlugin(),
                        _ -> triggerSteal.remove(carrier.getUniqueId()),
                        () -> triggerSteal.add(carrier.getUniqueId()));
            }

            Particle.DUST.builder()
                    .location(target.getEyeLocation().clone().subtract(0, 0.5, 0))
                    .offset(0.3, 0.3, 0.3)
                    .count(30)
                    .color(Color.fromRGB(0xc9ad70))
                    .receivers(16)
                    .spawn();
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_HUSK_DEATH, 0.3f, 2f);
        }

        private void applyExhaustBonus(LivingEntity livingEntity, boolean positive) {
            AttributeInstance movSpeed = livingEntity.getAttribute(Attribute.MOVEMENT_SPEED);
            if (movSpeed == null)
                return;

            AttributeModifier oldMod = movSpeed.getModifier(MOB_EXHAUST_SPEED_KEY);
            int factor = positive ? 1 : -1;

            if (oldMod == null) {
                movSpeed.addModifier(new AttributeModifier(MOB_EXHAUST_SPEED_KEY, EXHAUST_SPEED_AMOUNT * factor, AttributeModifier.Operation.ADD_SCALAR));
                return;
            }

            double newBonus = positive
                    ? Math.min(oldMod.getAmount() + EXHAUST_SPEED_AMOUNT, soulType().infused_speed_stacks.cached().floatValue() * EXHAUST_SPEED_AMOUNT)
                    : Math.max(oldMod.getAmount() - EXHAUST_SPEED_AMOUNT, soulType().infused_speed_stacks.cached().floatValue() * EXHAUST_SPEED_AMOUNT * factor);

            movSpeed.removeModifier(MOB_EXHAUST_SPEED_KEY);
            movSpeed.addModifier(new AttributeModifier(MOB_EXHAUST_SPEED_KEY, newBonus, AttributeModifier.Operation.ADD_SCALAR));
        }
    }
}
