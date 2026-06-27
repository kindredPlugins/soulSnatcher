package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

//TODO: will be added later no clear niche yet, just fall damage reduction feels to simple and misses infusion behaviour
public class CatSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance<CatSoulType> create(LivingEntity carrier) {
        return new CatSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "cat_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.CAT;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "4fd10c8e75f67398c47587d25fc146f311c053cc5d0aeab8790bce36ee88f5f8";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Cat Soul", TextColor.color(0x5a9d12));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }

    public static class CatSoulInstance extends SoulInstance<CatSoulType> implements OnDamageReceivedTrigger {
        protected CatSoulInstance(LivingEntity carrier, CatSoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if(event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

            event.setCancelled(true);

            carrier.getWorld().spawnParticle(Particle.CLOUD, carrier.getLocation(), 100, 0, 0, 0, 0.1);
            carrier.getWorld().playSound(carrier, Sound.ENTITY_CAT_AMBIENT, 1f, 2f);
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {}
    }
}
