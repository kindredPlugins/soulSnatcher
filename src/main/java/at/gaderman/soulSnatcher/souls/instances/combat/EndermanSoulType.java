package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.souls.triggers.projectiles.OnHitByProjectileTrigger;
import at.gaderman.soulSnatcher.utils.BlockUtils;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
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
import java.util.Map;

@AutoService(SoulType.class)
public class EndermanSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<EndermanSoulType> create(LivingEntity carrier) {
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
                Component.text("When hit by a projectile ")
                        .append(Component.text("teleport ", NamedTextColor.LIGHT_PURPLE)),
                Component.text("a short distance away to evade")
        );
    }

    //region Config Values

    private static final String EVADE_COOLDOWN_CONFIG_ID = "evade_cooldown";

    private final ConfigOption<Integer> evadeCooldown = configOption(EVADE_COOLDOWN_CONFIG_ID, 1500, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                EVADE_COOLDOWN_CONFIG_ID, "Cooldown for evading a projectile in milliseconds (1000ms = 1s)"
        );
    }

    //endregion

    public static class EndermanSoulInstance extends SoulInstance<EndermanSoulType> implements OnHitByProjectileTrigger, OnDamageReceivedTrigger {
        protected EndermanSoulInstance(LivingEntity carrier, EndermanSoulType soulType) {
            super(carrier, soulType);
        }

        private long lastEvade;

        private static final NamespacedKey EVADED_PROJECTILE = new NamespacedKey(SoulSnatcher.getPlugin(), "evaded_by");

        @Override
        public void onHitByProjectile(LivingEntity carrier, Projectile projectile, ProjectileHitEvent event) {
            PersistentDataContainer pdc = projectile.getPersistentDataContainer();
            if (pdc.getOrDefault(EVADED_PROJECTILE, PersistentDataType.LIST.strings(), Collections.emptyList()).contains(carrier.getUniqueId().toString())) {
                event.setCancelled(true);
            }

            if (lastEvade >= System.currentTimeMillis() - soulType().evadeCooldown.cached()) return;

            lastEvade = System.currentTimeMillis();
            event.setCancelled(true);

            List<String> evadedList = pdc.getOrDefault(EVADED_PROJECTILE, PersistentDataType.LIST.strings(), new ArrayList<>());
            evadedList.add(carrier.getUniqueId().toString());
            pdc.set(EVADED_PROJECTILE, PersistentDataType.LIST.strings(), evadedList);

            carrier.getWorld().spawnParticle(Particle.REVERSE_PORTAL, carrier.getEyeLocation(), 40, 0.1, 0.7, 0.1, 0.5);
            carrier.getWorld().playSound(carrier, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            Location evadeLoc = BlockUtils.findSpreadLocation(carrier.getLocation(), 4, 2);
            if (evadeLoc != null) {
                carrier.teleport(evadeLoc);
                carrier.getWorld().spawnParticle(Particle.PORTAL, carrier.getEyeLocation(), 40, 0.1, 0.7, 0.1, 1);
                return;
            }

            event.getEntity().setVelocity(event.getEntity().getVelocity().multiply(-0.05));
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if (event.getDamageSource().getDamageType() == DamageType.ENDER_PEARL)
                event.setCancelled(true);
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
        }
    }
}
