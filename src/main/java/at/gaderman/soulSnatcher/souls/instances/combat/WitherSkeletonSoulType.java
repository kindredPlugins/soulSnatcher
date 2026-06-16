package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageDealtTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class WitherSkeletonSoulType extends SoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
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

        );
    }

    public static class WitherSkeletonSoulInstance extends SoulInstance implements OnDamageDealtTrigger {
        protected WitherSkeletonSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public void onDamageDealt(LivingEntity carrier, LivingEntity target, EntityDamageByEntityEvent event) {
            if(carrier instanceof Player && !(event.getDamager() instanceof Player)) return;

            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 0, true, true, true));
        }
    }
}
