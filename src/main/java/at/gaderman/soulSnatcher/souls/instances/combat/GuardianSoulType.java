package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.GuardianAttackGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class GuardianSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<GuardianSoulType> create(LivingEntity carrier) {
        return new GuardianSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "guardian_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.GUARDIAN;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "b8e725779c234c590cce854db5c10485ed8d8a33fa9b2bdc3424b68bb1380bed";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Guardian Soul", TextColor.color(0x668980));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Any melee hit will inflict the"),
                Component.text("Wither Effect ",
                                Style.style(
                                        TextColor.color(0x444444),
                                        ShadowColor.shadowColor(0x80222222)
                                ))
                        .append(Component.text("on the target.", Style.style(NamedTextColor.WHITE, ShadowColor.none())))
        );
    }

    //region Config Values

    private static final String CHARGE_BUFFER_CONFIG_ID = "charge_buffer";
    private static final String MAX_CHARGE_CONFIG_ID = "max_charge";
    private static final String CHARGE_TIME_CONFIG_ID = "charge_time";

    private final ConfigOption<Integer> chargeBuffer = configOption(CHARGE_BUFFER_CONFIG_ID, 1500, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Double> maxCharge = configOption(MAX_CHARGE_CONFIG_ID, 0.5, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Integer> chargeTime = configOption(CHARGE_TIME_CONFIG_ID, 5000, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                CHARGE_BUFFER_CONFIG_ID, "The amount of time before the attack begins charging in milliseconds (1000ms = 1s)",
                MAX_CHARGE_CONFIG_ID, "The attack multiplier which can be reached through charging (0.5 -> 150% damage)",
                CHARGE_TIME_CONFIG_ID, "The time it takes to fully charge the attack in milliseconds (1000ms = 1s)"
        );
    }

    //endregion

    public static class GuardianSoulInstance extends SoulInstance<GuardianSoulType> implements OnDamageDealtTrigger {
        protected GuardianSoulInstance(LivingEntity carrier, GuardianSoulType soulType) {
            super(carrier, soulType);

            if(carrier instanceof Mob mob){
                Bukkit.getMobGoals().addGoal(mob, 0, new GuardianAttackGoal(mob));
            }

            lastAttack = System.currentTimeMillis();
        }

        private long lastAttack;
        private BukkitTask notifyChargeBegins;
        private BukkitTask notifyFullCharge;

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if (!(carrier instanceof Player)) return;
            if(!event.getDamager().equals(carrier)) return;

            int chargeBuffer = soulType().chargeBuffer.cached();
            int chargeTime = soulType().chargeTime.cached();
            double maxCharge = soulType().maxCharge.cached();
            try {
                long timeSinceLastAttack = System.currentTimeMillis() - lastAttack;

                if (timeSinceLastAttack < chargeBuffer)
                    return;

                double charge = Math.min((double) timeSinceLastAttack / chargeTime * maxCharge, maxCharge);

                event.setDamage(event.getDamage() * (1 + charge));

                target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getEyeLocation(), 30, 0.35, 0.5, 0.35, 0.35);
                target.getWorld().playSound(target, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1.25f + (float) charge);
            } finally {
                lastAttack = System.currentTimeMillis();

                if (notifyChargeBegins != null)
                    notifyChargeBegins.cancel();

                if (notifyFullCharge != null)
                    notifyFullCharge.cancel();

                notifyChargeBegins = Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
                    if (!carrier.isValid())
                        return;

                    carrier.getWorld().spawnParticle(Particle.DUST, carrier.getLocation().clone().add(0, 2, 0),
                            1, 0, 0, 0, 0, new Particle.DustOptions(Color.AQUA, 1));
                    carrier.getWorld().playSound(carrier, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 1f);
                }, chargeBuffer / 50);

                notifyFullCharge = Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
                    if (!carrier.isValid())
                        return;

                    carrier.getWorld().playSound(carrier, Sound.ENTITY_GUARDIAN_AMBIENT, 1f, 1.8f);
                    carrier.getWorld().spawnParticle(Particle.ENCHANTED_HIT, carrier.getEyeLocation(), 30, 0.35, 0.5, 0.35, 0);
                }, (chargeBuffer + chargeTime) / 50);
            }
        }

        @Override
        protected void cleanUp() {
            if (notifyChargeBegins != null)
                notifyChargeBegins.cancel();

            if (notifyFullCharge != null)
                notifyFullCharge.cancel();
        }
    }
}
