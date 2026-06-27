package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class WitherSkeletonSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<WitherSkeletonSoulType> create(LivingEntity carrier) {
        return new WitherSkeletonSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "wither_skeleton_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.WITHER_SKELETON;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "1e4d204ebc242eca2148f5853e3af00f84f0d674099dc394f6d2924b240ca2e3";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Wither Skeleton Soul", TextColor.color(0x4b4d4d));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Any melee hit will inflict the"),
                Component.text("Wither Effect ",
                                Style.style(
                                        TextColor.color(0x444444),
                                        ShadowColor.shadowColor(0x80222222)
                                ))
                        .append(Component.text("on the target.", Style.style(NamedTextColor.WHITE, ShadowColor.none())))
        );
    }

    //region Config Values

    private static final String WITHER_DURATION_CONFIG_ID = "wither_duration";
    private static final String WITHER_AMPLIFIER_CONFIG_ID = "wither_amplifier";

    private final ConfigOption<Integer> witherDuration = configOption(WITHER_DURATION_CONFIG_ID, 200, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Integer> witherAmplifier = configOption(WITHER_AMPLIFIER_CONFIG_ID, 0, FileConfiguration::getInt, value -> Math.min(Math.max(value, 0), 255));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                WITHER_DURATION_CONFIG_ID, "Wither duration from melee hits in ticks (20 ticks = 1 second)",
                WITHER_AMPLIFIER_CONFIG_ID, "Wither level as (level - 1) starting from 0 (Poison I)"
        );
    }

    //endregion

    public static class WitherSkeletonSoulInstance extends SoulInstance<WitherSkeletonSoulType> implements OnDamageDealtTrigger {
        protected WitherSkeletonSoulInstance(LivingEntity carrier, WitherSkeletonSoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if (carrier instanceof Player && !(event.getDamager() instanceof Player)) return;

            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, soulType().witherDuration.cached(), soulType().witherAmplifier.cached(), true, true, true));
        }
    }
}
