package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.AttributeSoul;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Random;

@AutoService(SoulType.class)
public class HorseSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<HorseSoulType> create(LivingEntity carrier) {
        return new HorseSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "horse_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.HORSE;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.ATTRIBUTES;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "85ce194a54315acc3bf9db7edf6e7da29f49524b1b8af0ef9e4ac3df2280b0d8";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Horse Soul", TextColor.color(0x3a1805));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Rolls random movement stats."),
                Component.text("Gain up to ")
                        .append(Component.text(maxMovementBonus.cached() + " Speed ", NamedTextColor.GOLD))
                        .append(Component.text("and ", NamedTextColor.WHITE))
                        .append(Component.text(maxJumpBonus.cached() + " Jump Strength", NamedTextColor.GOLD))
                        .append(Component.text(".", NamedTextColor.WHITE))
        );
    }

    @Override
    public boolean canOverwriteItself() {
        return true;
    }

    //region Config Values

    private static final String MAX_MOVEMENT_BONUS_CONFIG_ID = "max_movement_bonus";

    private final ConfigOption<Double> maxMovementBonus = configOption(MAX_MOVEMENT_BONUS_CONFIG_ID, 0.05, FileConfiguration::getDouble);
    private final ConfigOption<Double> maxJumpBonus = configOption("max_jump_bonus", 0.2, FileConfiguration::getDouble);

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(MAX_MOVEMENT_BONUS_CONFIG_ID, "Max amount of movement bonus a player can obtain from a bonus roll from this soul");
    }

    //endregion

    public static class HorseSoulInstance extends AttributeSoul<HorseSoulType> {
        protected HorseSoulInstance(LivingEntity carrier, HorseSoulType soulType) {
            super(carrier, soulType);

            if (carrier instanceof Player player) {
                double movBonus = player.getAttribute(Attribute.MOVEMENT_SPEED).getModifier(attributeKey).getAmount();
                double jumpBonus = player.getAttribute(Attribute.JUMP_STRENGTH).getModifier(attributeKey).getAmount();

                double movRatio = Math.min(1.0, movBonus / soulType.maxMovementBonus.cached());
                Component subtitle = createSubtitle(jumpBonus, movBonus, movRatio);

                player.sendTitlePart(TitlePart.TITLE, Component.text(""));
                player.sendTitlePart(TitlePart.SUBTITLE, subtitle);

                player.playSound(player.getLocation(), Sound.ENTITY_HORSE_AMBIENT, 1f, (float) (movRatio * 2));
            }
        }

        private @NotNull Component createSubtitle(double jumpBonus, double movBonus, double movRatio) {
            double jumpRatio = Math.min(1.0, jumpBonus / soulType().maxJumpBonus.cached());

            String displayMovBonus = String.format("%.3f", movBonus);
            String displayJumpBonus = String.format("%.3f", jumpBonus);

            return Component.text("⚡ Speed ", NamedTextColor.GREEN)
                    .append(Component.text("+" + displayMovBonus, NamedTextColor.GOLD))
                    .append(Component.text(" (" + Math.round(movRatio * 100) + "%)", NamedTextColor.GRAY))

                    .append(Component.text("  │  ", NamedTextColor.DARK_GRAY))

                    .append(Component.text("⤴ Jump ", NamedTextColor.GREEN))
                    .append(Component.text("+" + displayJumpBonus, NamedTextColor.GOLD))
                    .append(Component.text(" (" + Math.round(jumpRatio * 100) + "%)", NamedTextColor.GRAY)
                    );
        }

        private static final NamespacedKey MOD_MOV_SEED = new NamespacedKey(SoulSnatcher.getPlugin(), "horse_soul_mov_mod_seed");
        private static final NamespacedKey MOD_JUMP_SEED = new NamespacedKey(SoulSnatcher.getPlugin(), "horse_soul_jump_mod_seed");

        @Override
        public Map<Attribute, AttributeModifier> attributeModifierMap() {
            PersistentDataContainer pdc = carrier().getPersistentDataContainer();

            long moveSeed = pdc.getOrDefault(MOD_MOV_SEED, PersistentDataType.LONG, (long) (Math.random() * Long.MAX_VALUE));
            long jumpSeed = pdc.getOrDefault(MOD_JUMP_SEED, PersistentDataType.LONG, (long) (Math.random() * Long.MAX_VALUE));

            pdc.set(MOD_MOV_SEED, PersistentDataType.LONG, moveSeed);
            pdc.set(MOD_JUMP_SEED, PersistentDataType.LONG, jumpSeed);

            double randomMov = (new Random(moveSeed).nextDouble() * soulType().maxMovementBonus.cached());
            double randomJump = (new Random(jumpSeed).nextDouble() * soulType().maxJumpBonus.cached());

            return Map.of(
                    Attribute.MOVEMENT_SPEED, createModifier(randomMov, AttributeModifier.Operation.ADD_NUMBER),
                    Attribute.JUMP_STRENGTH, createModifier(randomJump, AttributeModifier.Operation.ADD_NUMBER),
                    Attribute.STEP_HEIGHT, createModifier(0.5, AttributeModifier.Operation.ADD_NUMBER)
            );
        }

        @Override
        protected void reset() {
            super.reset();

            PersistentDataContainer pdc = carrier().getPersistentDataContainer();
            pdc.remove(MOD_MOV_SEED);
            pdc.remove(MOD_JUMP_SEED);
        }
    }
}
