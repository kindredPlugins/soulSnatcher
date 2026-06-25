package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class SheepSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new SheepSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "sheep_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SHEEP;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "84e5cdb0edb362cb454586d1fd0ebe971423f015b0b1bfc95f8d5af8afe7e810";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Sheep Soul", NamedTextColor.AQUA);
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When being hit reduce damage by"),
                Component.text((int) (absorptionAmount.defaultValue() * 100) + "% ", NamedTextColor.GOLD)
                        .append(Component.text("every " + absorbCooldown.cached() / 1000.0 + "s", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String ABSORPTION_AMOUNT_CONFIG_ID = "absorption_amount";
    private static final String ABSORB_COOLDOWN_CONFIG_ID = "absorb_cooldown";

    private final ConfigOption<Double> absorptionAmount = configOption(ABSORPTION_AMOUNT_CONFIG_ID, 0.5, FileConfiguration::getDouble);
    private final ConfigOption<Integer> absorbCooldown = configOption(ABSORB_COOLDOWN_CONFIG_ID, 15 * 1000, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                ABSORPTION_AMOUNT_CONFIG_ID, "How much percentage of the damage is absorbed with the ability (0.6 -> 60% damage absorbed 40% taken)",
                ABSORB_COOLDOWN_CONFIG_ID, "How long it takes to regrow the wool shield in milliseconds (1000ms = 1s)"
        );
    }

    //endregion

    public static class SheepSoulInstance extends SoulInstance<SheepSoulType> implements OnDamageReceivedTrigger {
        protected SheepSoulInstance(LivingEntity carrier, SheepSoulType soulType) {
            super(carrier, soulType);
        }

        private long lastHitAbsorb;

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if(lastHitAbsorb > System.currentTimeMillis() - soulType().absorbCooldown.cached()) return;

            lastHitAbsorb = System.currentTimeMillis();
            event.setDamage(event.getDamage() * (1 - soulType().absorptionAmount.cached()));

            carrier.getWorld().spawnParticle(Particle.DUST, carrier.getLocation().add(0, 0.8, 0), 50, 0.3, 0.5, 0.3, 0.2,
                    new Particle.DustOptions(Color.WHITE, 2));
            carrier.getWorld().playSound(carrier, Sound.BLOCK_WOOL_HIT, 3f, 0.5f);

            if(event.getDamageSource().getCausingEntity() instanceof LivingEntity livingEntity) {
                Vector knockbackDirection = carrier.getLocation().subtract(livingEntity.getLocation()).toVector();
                livingEntity.knockback(0.5, knockbackDirection.getX(), knockbackDirection.getZ());

                if(livingEntity instanceof Player player)
                    player.setVelocity(player.getVelocity().add(knockbackDirection.normalize().multiply(-0.2).setY(0.025)));
            }
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {}
    }
}
