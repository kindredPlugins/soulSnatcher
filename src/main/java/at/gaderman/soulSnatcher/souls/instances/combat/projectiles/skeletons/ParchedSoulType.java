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
public class ParchedSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new SkeletonSoulVariantInstance(carrier, this,
                new SkeletonSoulVariantInstance.Options(
                        "weakness_shot",
                        Color.fromRGB(0x3d423d),
                        Sound.ENTITY_PARCHED_STEP,
                        PotionEffectType.WEAKNESS, 30 * 20, 0
                )
        );
    }

    @Override
    public @NotNull String id() {
        return "parched_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.PARCHED;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "24aeceff5f26dd8413c5c03547c234ac03108d187af0b9cd834a8ce12598591c";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Parched Soul", TextColor.color(0xceb686));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }
}
