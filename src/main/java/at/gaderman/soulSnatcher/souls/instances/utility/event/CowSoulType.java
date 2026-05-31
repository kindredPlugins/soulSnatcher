package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.OnEntityPotionEffectTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class CowSoulType extends SoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new CowSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "cow_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.COW;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "8f8ce3915a21168d8394214552cdb5652e855e9e382001d596972fe3c009ea7d";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Cow Soul", TextColor.color(0x292017));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }

    public static class CowSoulInstance extends SoulInstance implements OnEntityPotionEffectTrigger {
        protected CowSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);
        }

        private boolean milkInProcess;

        @Override
        public void onEntityPotionEffect(LivingEntity carrier, EntityPotionEffectEvent event) {
            if(event.getNewEffect() == null) return;
            if(milkInProcess || event.getNewEffect().getType().getEffectCategory() != PotionEffectType.Category.HARMFUL) return;

            milkInProcess = true;
            carrier.getWorld().playSound(carrier, Sound.ENTITY_WANDERING_TRADER_DRINK_MILK, 1f, 1f);

            SoulSnatcher.getPlugin().registerDelayedTask(() -> {
                milkInProcess = false;

                if(!carrier.isValid()) return;

                carrier.getActivePotionEffects().stream()
                        .filter(effect -> effect.getType().getCategory() == PotionEffectTypeCategory.HARMFUL)
                        .forEach(effect -> carrier.removePotionEffect(effect.getType()));

                carrier.getWorld().spawnParticle(Particle.END_ROD, carrier.getLocation().add(0, 1, 0), 30, 0.2, 0.2, 0.2, 0.1);
                carrier.getWorld().playSound(carrier, Sound.ENTITY_SPLASH_POTION_THROW, 1f, 2f);
            }, 15L);
        }
    }
}
