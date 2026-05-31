package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class SheepSoulType extends SoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new SheepSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "sheep_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SHEEP;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "84e5cdb0edb362cb454586d1fd0ebe971423f015b0b1bfc95f8d5af8afe7e810";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Sheep Soul", NamedTextColor.AQUA);
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When being hit reduce damage by"),
                Component.text((int) (ABSORPTION_AMOUNT * 100) + "%", NamedTextColor.GOLD)
        );
    }

    private static final double ABSORPTION_AMOUNT = 0.5;
    private static final long ABSORB_COOLDOWN = 15 * 1000L;

    public static class SheepSoulInstance extends SoulInstance implements OnDamageReceivedTrigger {
        protected SheepSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);
        }

        private long lastHitAbsorb;

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if(lastHitAbsorb > System.currentTimeMillis() - ABSORB_COOLDOWN) return;

            lastHitAbsorb = System.currentTimeMillis();
            event.setDamage(event.getDamage() * (1 - ABSORPTION_AMOUNT));

            carrier.getWorld().spawnParticle(Particle.DUST, carrier.getLocation().add(0, 0.8, 0), 50, 0.3, 0.5, 0.3, 0.2,
                    new Particle.DustOptions(Color.WHITE, 2));
            carrier.getWorld().playSound(carrier, Sound.BLOCK_WOOL_HIT, 3f, 0.5f);

            if(event.getDamageSource().getCausingEntity() instanceof LivingEntity livingEntity) {
                Vector knockbackDirection = carrier.getLocation().subtract(livingEntity.getLocation()).toVector();
                livingEntity.knockback(0.5, knockbackDirection.getX(), knockbackDirection.getZ());

                if(livingEntity instanceof Player player)
                    player.setVelocity(player.getVelocity().add(knockbackDirection.normalize().multiply(-0.2).setY(0.025)));
            }
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {}
    }
}
