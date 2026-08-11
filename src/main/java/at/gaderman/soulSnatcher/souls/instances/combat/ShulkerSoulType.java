package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class ShulkerSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<ShulkerSoulType> create(LivingEntity carrier) {
        return new ShulkerSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "shulker_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SHULKER;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "1e73832e272f8844c476846bc424a3432fb698c58e6ef2a9871c7d29aeea7";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Shulker Soul", TextColor.color(0x673a7b));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
            Component.text("When attacking or being attacked"),
                Component.text("launch a ")
                        .append(Component.text("shulker bullet ", NamedTextColor.DARK_PURPLE))
                        .append(Component.text("every ", NamedTextColor.WHITE))
                        .append(Component.text(triggerCooldown.cached() / 1000.0 + "s", NamedTextColor.AQUA))
                        .append(Component.text(".", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String TRIGGER_COOLDOWN_CONFIG_ID = "trigger_cooldown";

    private final ConfigOption<Integer> triggerCooldown = configOption(TRIGGER_COOLDOWN_CONFIG_ID, 2000, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                TRIGGER_COOLDOWN_CONFIG_ID, "Cooldown for healing when taking fire/lava damage (1000ms = 1s)"
        );
    }

    //endregion

    public static class ShulkerSoulInstance extends SoulInstance<ShulkerSoulType> implements OnDamageDealtTrigger, OnDamageReceivedTrigger {
        protected ShulkerSoulInstance(LivingEntity carrier, ShulkerSoulType soulType) {
            super(carrier, soulType);
        }

        private long lastHit;

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if(lastHit > System.currentTimeMillis() - soulType().triggerCooldown.cached())
                return;

            shootShulkerShell(carrier, target);
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            if(lastHit > System.currentTimeMillis() - soulType().triggerCooldown.cached())
                return;

            shootShulkerShell(carrier, damager);
        }

        private void shootShulkerShell(LivingEntity carrier, LivingEntity target) {
            lastHit = System.currentTimeMillis();

            carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1f, 1f);

            Vector launchDirection = carrier.getLocation().getDirection().normalize().multiply(0.5).setY(0.5);
            carrier.launchProjectile(ShulkerBullet.class, launchDirection, bullet -> {
               bullet.setTarget(target);
            });
        }
    }
}
