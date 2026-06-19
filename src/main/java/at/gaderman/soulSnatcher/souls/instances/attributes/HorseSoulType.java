package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
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
public class HorseSoulType extends SoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
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
                Component.text("Rolls random movement stats.")
        );
    }

    @Override
    public boolean canOverwriteItself() {
        return true;
    }

    public static class HorseSoulInstance extends AttributeSoul {
        protected HorseSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);

            if (carrier instanceof Player player) {
                double movBonus = player.getAttribute(Attribute.MOVEMENT_SPEED).getModifier(attributeKey).getAmount();
                double jumpBonus = player.getAttribute(Attribute.JUMP_STRENGTH).getModifier(attributeKey).getAmount();

                double movRatio = Math.min(1.0, movBonus / MAX_MOVEMENT_BONUS);
                double jumpRatio = Math.min(1.0, jumpBonus / MAX_JUMP_BONUS);

                String displayMovBonus = String.format("%.3f", movBonus);
                String displayJumpBonus = String.format("%.3f", jumpBonus);

                int movBars = (int) Math.round(movRatio * 10);
                int jumpBars = (int) Math.round(jumpRatio * 10);

                Component subtitle =
                        Component.text("⚡ Speed ", NamedTextColor.GREEN)
                                .append(Component.text("+" + displayMovBonus, NamedTextColor.GOLD))
                                .append(Component.text(" (" + Math.round(movRatio * 100) + "%)", NamedTextColor.GRAY))

                                .append(Component.text("  │  ", NamedTextColor.DARK_GRAY))

                                .append(Component.text("⤴ Jump ", NamedTextColor.GREEN))
                                .append(Component.text("+" + displayJumpBonus, NamedTextColor.GOLD))
                                .append(Component.text(" (" + Math.round(jumpRatio * 100) + "%)", NamedTextColor.GRAY)
                                );

                player.sendTitlePart(TitlePart.TITLE, Component.text(""));
                player.sendTitlePart(TitlePart.SUBTITLE, subtitle);

                player.playSound(player, Sound.ENTITY_HORSE_AMBIENT, 1f, (float) (movRatio * 2));
            }
        }

        private static final NamespacedKey MOD_MOV_SEED = new NamespacedKey(SoulSnatcher.getPlugin(), "horse_soul_mov_mod_seed");
        private static final NamespacedKey MOD_JUMP_SEED = new NamespacedKey(SoulSnatcher.getPlugin(), "horse_soul_jump_mod_seed");

        private static final double MAX_MOVEMENT_BONUS = 0.05;
        private static final double MAX_JUMP_BONUS = 0.2;

        @Override
        public Map<Attribute, AttributeModifier> attributeModifierMap() {
            PersistentDataContainer pdc = carrier().getPersistentDataContainer();

            long moveSeed = pdc.getOrDefault(MOD_MOV_SEED, PersistentDataType.LONG, (long) (Math.random() * Long.MAX_VALUE));
            long jumpSeed = pdc.getOrDefault(MOD_JUMP_SEED, PersistentDataType.LONG, (long) (Math.random() * Long.MAX_VALUE));

            pdc.set(MOD_MOV_SEED, PersistentDataType.LONG, moveSeed);
            pdc.set(MOD_JUMP_SEED, PersistentDataType.LONG, jumpSeed);

            double randomMov = (new Random(moveSeed).nextDouble() * MAX_MOVEMENT_BONUS);
            double randomJump = (new Random(jumpSeed).nextDouble() * MAX_JUMP_BONUS);

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
