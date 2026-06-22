package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.AttributeSoul;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class ChickenSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<ChickenSoulType> create(LivingEntity carrier) {
        return new ChickenSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "chicken_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.CHICKEN;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.ATTRIBUTES;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "3ad3dd0083faa69a062f9ad81418f5a596180bf1592e4b8d1303b230b64bc79e";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Chicken Soul", TextColor.color(0xff0000));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Adds various ")
                        .append(Component.text("lightweight ", NamedTextColor.AQUA))
                        .append(Component.text("attributes", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String GRAVITY_MOD_CONFIG_ID = "gravity_mod";
    private static final String MOVEMENT_SPEED_MOD_CONFIG_ID = "movement_speed_mod";
    private static final String SAFE_FALL_DISTANCE_CONFIG_ID = "safe_fall_distance";
    private static final String FALL_DAMAGE_MULTIPLIER_CONFIG_ID = "fall_damage_multiplier";

    private final ConfigOption<Double> gravityMod = configOption(GRAVITY_MOD_CONFIG_ID, -0.2, FileConfiguration::getDouble);
    private final ConfigOption<Double> movementSpeedMod = configOption(MOVEMENT_SPEED_MOD_CONFIG_ID, 0.05, FileConfiguration::getDouble);
    private final ConfigOption<Double> safeFallDistance = configOption(SAFE_FALL_DISTANCE_CONFIG_ID, 2.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> fallDamageMultiplier = configOption(FALL_DAMAGE_MULTIPLIER_CONFIG_ID, -0.25, FileConfiguration::getDouble);

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                GRAVITY_MOD_CONFIG_ID, "Gravity modifier which is applied percentage based (-0.2 => -20% Gravity)",
                MOVEMENT_SPEED_MOD_CONFIG_ID, "Applied to movement speed as MULTIPLY_SCALAR_1 meaning it will multiply the total speed (including other modifiers) by that amount"
        );
    }

    //endregion

    //TODO: monsters do not take much profit from this need extra behaviour - maybe more drastic attributes?
    public static class ChickenSoulInstance extends AttributeSoul<ChickenSoulType> {

        protected ChickenSoulInstance(LivingEntity carrier, ChickenSoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public Map<Attribute, AttributeModifier> attributeModifierMap() {
            return Map.of(
                    Attribute.GRAVITY, createModifier(soulType().gravityMod.cached(), AttributeModifier.Operation.ADD_SCALAR),
                    Attribute.MOVEMENT_SPEED, createModifier(soulType().movementSpeedMod.cached(), AttributeModifier.Operation.MULTIPLY_SCALAR_1),
                    Attribute.SAFE_FALL_DISTANCE, createModifier(soulType().safeFallDistance.cached(), AttributeModifier.Operation.ADD_NUMBER),
                    Attribute.FALL_DAMAGE_MULTIPLIER, createModifier(soulType().fallDamageMultiplier.cached(), AttributeModifier.Operation.ADD_NUMBER)
            );
        }
    }
}
