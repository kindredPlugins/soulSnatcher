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
public class StraySoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new SkeletonSoulVariantInstance(carrier, this,
                new SkeletonSoulVariantInstance.Options(
                        "slow_shot",
                        Color.GRAY,
                        Sound.ENTITY_STRAY_STEP,
                        PotionEffectType.SLOWNESS, 30 * 20, 0
                )
        );
    }

    @Override
    public @NotNull String id() {
        return "stray_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.STRAY;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "78ddf76e555dd5c4aa8a0a5fc584520cd63d489c253de969f7f22f85a9a2d56";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Stray Soul", TextColor.color(0x576c6d));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }
}
