package at.gaderman.soulSnatcher.souls.instances.combat.projectiles.skeletons;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class BoggedSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new SkeletonSoulVariantInstance(carrier, this,
                new SkeletonSoulVariantInstance.Options(
                        "poison_shot",
                        Color.fromRGB(0x5b6e42),
                        Sound.ENTITY_BOGGED_STEP,
                        PotionEffectType.POISON, 11 * 20, 0
                )
        );
    }

    @Override
    public @NotNull String id() {
        return "bogged_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.BOGGED;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "a3b9003ba2d05562c75119b8a62185c67130e9282f7acbac4bc2824c21eb95d9";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Bogged Soul", TextColor.color(0x5e6d31));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }
}
