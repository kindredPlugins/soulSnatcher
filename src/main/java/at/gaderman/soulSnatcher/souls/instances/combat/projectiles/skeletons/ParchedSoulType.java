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
public class ParchedSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<SoulType> create(LivingEntity carrier) {
        return new SkeletonSoulVariantInstance(carrier, this,
                new SkeletonSoulVariantInstance.Options(
                        "weakness_shot",
                        Color.fromRGB(0x3d423d),
                        Sound.ENTITY_PARCHED_STEP,
                        PotionEffectType.WEAKNESS, weaknessDuration.cached(), weaknessAmplifier.cached()
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
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Any ")
                        .append(Component.text("projectile ", NamedTextColor.AQUA))
                        .append(Component.text("fired will inflict ")),
                Component.text("Weakness ", NamedTextColor.GRAY)
                        .append(Component.text("on a direct hit", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String WEAKNESS_DURATION_CONFIG_ID = "weakness_duration";
    private static final String WEAKNESS_AMPLIFIER_CONFIG_ID = "weakness_amplifier";

    private final ConfigOption<Integer> weaknessDuration = configOption(WEAKNESS_DURATION_CONFIG_ID, 30 * 20, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Integer> weaknessAmplifier = configOption(WEAKNESS_AMPLIFIER_CONFIG_ID, 0, FileConfiguration::getInt, value -> Math.min(Math.max(value, 0), 255));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                WEAKNESS_DURATION_CONFIG_ID, "Weakness duration from afflicted projectiles on hit in ticks (20 ticks = 1 second)",
                WEAKNESS_AMPLIFIER_CONFIG_ID, "Weakness level as (level - 1) starting from 0 (Weakness I)"
        );
    }

    //endregion
}
