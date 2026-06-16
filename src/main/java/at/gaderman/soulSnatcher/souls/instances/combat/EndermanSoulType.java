package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnHitByProjectileTrigger;
import at.gaderman.soulSnatcher.utils.BlockUtils;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoService(SoulType.class)
public class EndermanSoulType extends SoulType {

    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new EndermanSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "enderman_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.ENDERMAN;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "c39c2784d14c06f84ce41c5883aba932824340b2e7e673d7bf83a521de71135";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Enderman Soul", TextColor.color(0xf9abff));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(

        );
    }

    public static class EndermanSoulInstance extends SoulInstance implements OnHitByProjectileTrigger, OnDamageReceivedTrigger {
        protected EndermanSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);
        }

        private static final int EVADE_COOLDOWN = 1500;
        private long lastEvade;

        private static final NamespacedKey EVADED_PROJECTILE = new NamespacedKey(SoulSnatcher.getPlugin(), "evaded_by");

        @Override
        public void onHitByProjectile(LivingEntity carrier, Projectile projectile, ProjectileHitEvent event) {
            PersistentDataContainer pdc = projectile.getPersistentDataContainer();
            if(pdc.getOrDefault(EVADED_PROJECTILE, PersistentDataType.LIST.strings(), Collections.emptyList()).contains(carrier.getUniqueId().toString())){
                event.setCancelled(true);
            }

            if(lastEvade >= System.currentTimeMillis() - EVADE_COOLDOWN) return;

            lastEvade = System.currentTimeMillis();
            event.setCancelled(true);

            List<String> evadedList = pdc.getOrDefault(EVADED_PROJECTILE, PersistentDataType.LIST.strings(), new ArrayList<>());
            evadedList.add(carrier.getUniqueId().toString());
            pdc.set(EVADED_PROJECTILE, PersistentDataType.LIST.strings(), evadedList);

            carrier.getWorld().spawnParticle(Particle.REVERSE_PORTAL, carrier.getEyeLocation(), 40, 0.1, 0.7, 0.1, 0.5);
            carrier.getWorld().playSound(carrier, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            Location evadeLoc = BlockUtils.findSpreadLocation(carrier.getLocation(), 4, 2);
            if(evadeLoc != null){
                carrier.teleport(evadeLoc);
                carrier.getWorld().spawnParticle(Particle.PORTAL, carrier.getEyeLocation(), 40, 0.1, 0.7, 0.1, 1);
                return;
            }

            event.getEntity().setVelocity(event.getEntity().getVelocity().multiply(-0.05));
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if(event.getDamageSource().getDamageType() == DamageType.ENDER_PEARL)
                event.setCancelled(true);
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {}
    }
}
