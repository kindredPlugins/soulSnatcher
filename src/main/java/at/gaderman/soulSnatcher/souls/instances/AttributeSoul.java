package at.gaderman.soulSnatcher.souls.instances;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

public abstract class AttributeSoul<T extends SoulType> extends SoulInstance<T> {

    protected final NamespacedKey attributeKey = new NamespacedKey(SoulSnatcher.getPlugin(),
            "soul_attributes/" + soulType().id());

    protected AttributeSoul(LivingEntity carrier, T soulType) {
        super(carrier, soulType);

        cleanUp();
        attributeModifierMap().forEach(((attribute, modifier) ->
                carrier.getAttribute(attribute).addModifier(modifier)));
    }

    public abstract Map<Attribute, AttributeModifier> attributeModifierMap();

    protected final AttributeModifier createModifier(double amount, AttributeModifier.Operation operation){
        return new AttributeModifier(attributeKey, amount, operation);
    }

    @Override
    protected final void cleanUp() {
        attributeModifierMap().forEach(((attribute, modifier) -> {
            carrier().getAttribute(attribute).removeModifier(modifier.key());
        }));
        extraCleanUp();
    }

    /**
     * Used for additional cleanUp, as the default one is overlapped with removing modifiers
     */
    protected void extraCleanUp(){}
}
