package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.action.OnTargetTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class MagmaCubeSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<MagmaCubeSoulType> create(LivingEntity carrier) {
        return new MagmaCubeSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "magma_cube_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.MAGMA_CUBE;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "a1c97a06efde04d00287bf20416404ab2103e10f08623087e1b0c1264a1c0f0c";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Magma Cube Soul", TextColor.color(0xcb3d07));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When damaged by fire or lava ")
                        .append(Component.text("resist ", NamedTextColor.GOLD)),
                Component.text("and heal by ")
                        .append(Component.text(healAmount.cached(), NamedTextColor.GREEN))
                        .append(Component.text("❤", NamedTextColor.RED))
                        .append(Component.text(".", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String HEALING_COOLDOWN_CONFIG_ID = "healing_cooldown";
    private static final String HEAL_AMOUNT_CONFIG_ID = "healing_amount";

    private final ConfigOption<Integer> healingCooldown = configOption(HEALING_COOLDOWN_CONFIG_ID, 1000, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Double> healAmount = configOption(HEAL_AMOUNT_CONFIG_ID, 0.5, FileConfiguration::getDouble, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                HEALING_COOLDOWN_CONFIG_ID, "Cooldown for healing when taking fire/lava damage (1000ms = 1s)",
                HEAL_AMOUNT_CONFIG_ID, "Amount to be healed in HP"
        );
    }

    //endregion


    @Override
    public boolean isInvalidInfusionTarget(LivingEntity entity) {
        return super.isInvalidInfusionTarget(entity) || entity instanceof Blaze || entity instanceof Ghast
                || entity instanceof PigZombie || entity instanceof Strider || entity instanceof WitherSkeleton
                || entity instanceof Warden;
    }

    public static class MagmaCubeSoulInstance extends SoulInstance<MagmaCubeSoulType> implements OnDamageReceivedTrigger, OnTargetTrigger {
        protected MagmaCubeSoulInstance(LivingEntity carrier, MagmaCubeSoulType soulType) {
            super(carrier, soulType);
        }

        private long lastHeal;

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK && event.getCause() != EntityDamageEvent.DamageCause.FIRE
                    && event.getCause() != EntityDamageEvent.DamageCause.LAVA)
                return;

            event.setCancelled(true);

            if (lastHeal < System.currentTimeMillis() - soulType().healingCooldown.cached()) {
                carrier.getWorld().spawnParticle(Particle.FLAME, carrier.getLocation().add(0, 1, 0), 30, 0, 0.5, 0, 0.1);
                carrier.getWorld().playSound(carrier.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 1f, 2f);

                lastHeal = System.currentTimeMillis();
                carrier.heal(soulType().healAmount.cached(), EntityRegainHealthEvent.RegainReason.REGEN);
            }
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
        }

        @Override
        public void onBeingTargeted(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event) {}

        @Override
        public void onCarrierTarget(LivingEntity carrier, LivingEntity target, EntityTargetLivingEntityEvent event) {
            carrier.setFireTicks(400);
        }
    }
}
