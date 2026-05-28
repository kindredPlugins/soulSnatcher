package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.AttributeSoul;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class ChickenSoulType extends SoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
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
    protected @NotNull String skullTexture() {
        return "http://textures.minecraft.net/texture/3ad3dd0083faa69a062f9ad81418f5a596180bf1592e4b8d1303b230b64bc79e";
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

    //TODO: monsters do not take much profit from this need extra behaviour - maybe more drastic attributes?
    public static class ChickenSoulInstance extends AttributeSoul {

        protected ChickenSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public Map<Attribute, AttributeModifier> attributeModifierMap() {
            return Map.of(
                    Attribute.GRAVITY, createModifier(-0.2, AttributeModifier.Operation.ADD_SCALAR),
                    Attribute.MOVEMENT_SPEED, createModifier(+0.05, AttributeModifier.Operation.MULTIPLY_SCALAR_1),
                    Attribute.SAFE_FALL_DISTANCE, createModifier(+2, AttributeModifier.Operation.ADD_NUMBER),
                    Attribute.FALL_DAMAGE_MULTIPLIER, createModifier(-0.25, AttributeModifier.Operation.ADD_NUMBER)
            );
        }
    }
}
