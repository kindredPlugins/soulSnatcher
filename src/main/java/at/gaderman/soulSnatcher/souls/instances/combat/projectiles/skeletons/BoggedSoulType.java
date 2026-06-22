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
public class BoggedSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<SoulType> create(LivingEntity carrier) {
        return new SkeletonSoulVariantInstance(carrier, this,
                new SkeletonSoulVariantInstance.Options(
                        "poison_shot",
                        Color.fromRGB(0x5b6e42),
                        Sound.ENTITY_BOGGED_STEP,
                        PotionEffectType.POISON, poisonDuration.cached(), poisonAmplifier.cached()
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
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Any ")
                        .append(Component.text("projectile ", NamedTextColor.AQUA))
                        .append(Component.text("fired will inflict ")),
                Component.text("Poison ", TextColor.color(0x5e6d31))
                                .append(Component.text("on a direct hit", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String POISON_DURATION_CONFIG_ID = "poison_duration";
    private static final String POISON_AMPLIFIER_CONFIG_ID = "poison_amplifier";

    private final ConfigOption<Integer> poisonDuration = configOption(POISON_DURATION_CONFIG_ID, 11 * 20, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Integer> poisonAmplifier = configOption(POISON_AMPLIFIER_CONFIG_ID, 0, FileConfiguration::getInt, value -> Math.min(Math.max(value, 0), 255));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                POISON_DURATION_CONFIG_ID, "Poison duration from afflicted projectiles on hit in ticks (20 ticks = 1 second)",
                POISON_AMPLIFIER_CONFIG_ID, "Poison level as (level - 1) starting from 0 (Poison I)"
        );
    }

    //endregion
}
