package at.gaderman.soulSnatcher.souls.instances.combat.projectiles.skeletons;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class StraySoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<SoulType> create(LivingEntity carrier) {
        return new SkeletonSoulVariantInstance(carrier, this,
                new SkeletonSoulVariantInstance.Options(
                        "slow_shot",
                        Color.GRAY,
                        Sound.ENTITY_STRAY_STEP,
                        PotionEffectType.SLOWNESS, slowDuration.cached(), slowAmplifier.cached()
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
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Any ")
                        .append(Component.text("projectile ", NamedTextColor.AQUA))
                        .append(Component.text("fired will inflict ")),
                Component.text("Slowness ", TextColor.color(0x6a86ab))
                        .append(Component.text("on a direct hit.", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String SLOW_DURATION_CONFIG_ID = "slow_duration";
    private static final String SLOW_AMPLIFIER_CONFIG_ID = "slow_amplifier";

    private final ConfigOption<Integer> slowDuration = configOption(SLOW_DURATION_CONFIG_ID, 30 * 20, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Integer> slowAmplifier = configOption(SLOW_AMPLIFIER_CONFIG_ID, 0, FileConfiguration::getInt, value -> Math.min(Math.max(value, 0), 255));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                SLOW_DURATION_CONFIG_ID, "Slowness duration from afflicted projectiles on hit in ticks (20 ticks = 1 second)",
                SLOW_AMPLIFIER_CONFIG_ID, "Slowness level as (level - 1) starting from 0 (Slowness I)"
        );
    }

    //endregion
}
