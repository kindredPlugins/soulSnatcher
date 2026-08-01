package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.AttributeSoul;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class IronGolemSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<IronGolemSoulType> create(LivingEntity carrier) {
        return new IronGolemSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "iron_golem_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.IRON_GOLEM;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.ATTRIBUTES;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "4271913a3fc8f56bdf6b90a4b4ed6a05c562ce0076b5344d444fb2b040ae57d";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Iron Golem Soul", NamedTextColor.GRAY);
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Grants knockback resistance."),
                Component.text("Non-crit melee attacks throw targets in the air.")
        );
    }

    //region Config values

    private static final String UPWARDS_THROW_CONFIG_ID = "upwards_throw_enabled";

    private final ConfigOption<Boolean> isUpwardsThrowEnabled = configOption(UPWARDS_THROW_CONFIG_ID, true, FileConfiguration::getBoolean);

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(UPWARDS_THROW_CONFIG_ID, "If true non-critical hits will throw hit entities into the air similar to an iron golem");
    }

    //endregion

    public static class IronGolemSoulInstance extends AttributeSoul<IronGolemSoulType> implements OnDamageDealtTrigger {

        protected IronGolemSoulInstance(LivingEntity carrier, IronGolemSoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public Map<Attribute, AttributeModifier> attributeModifierMap() {
            return Map.of(
              Attribute.KNOCKBACK_RESISTANCE, createModifier(1, AttributeModifier.Operation.ADD_NUMBER)
            );
        }

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if(event.isCritical() || event.getDamageSource().isIndirect()) return;
            if(!soulType().isUpwardsThrowEnabled.cached()) return;

            Bukkit.getScheduler().runTask(SoulSnatcher.getPlugin(), () -> {
                var knockbackResistance = target.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                double factor = knockbackResistance == null ? 1 : Math.max(1 - knockbackResistance.getValue(), 0);

                target.setVelocity(target.getVelocity().add(new Vector(0, 0.4 * factor, 0)));
                carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);
            });
        }
    }
}
