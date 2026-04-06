package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.triggers.OnDamageReceivedTrigger;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Stream;

public class SoulListener implements Listener {

    @EventHandler
    public void onSoulRelease(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getKiller() == null) return;
        if (mob.getScoreboardTags().contains(Soul.NO_SOUL_RELEASE_TAG)) return;

        var optSoul = SoulRegistry.getInstance().getSoul(mob.getType());
        if (optSoul.isEmpty()) return;

        optSoul.get().releaseSoul(mob.getLocation(), mob.getKiller());
    }

    @EventHandler
    public void onSoulInfuse(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        Collection<Player> nearbyPlayers = mob.getWorld().getNearbyPlayers(mob.getLocation(), 50, 50);
        if (nearbyPlayers.isEmpty()) return;

        Player closestPlayer = nearbyPlayers
                .stream()
                .min(Comparator.comparing(player -> player.getLocation().distanceSquared(mob.getLocation())))
                .get();

        PersistentDataContainer pdc = closestPlayer.getPersistentDataContainer();
        List<String> unboundSoulIds = new ArrayList<>(pdc.getOrDefault(Soul.UNBOUND_SOULS, PersistentDataType.LIST.strings(), List.of()));
        var souls = unboundSoulIds.stream()
                .map(soulId -> SoulRegistry.getInstance().getSoul(soulId))
                .filter(Objects::nonNull)
                .filter(soul -> !soul.entityType().equals(mob.getType()))
                .toList();

        if (souls.isEmpty()) return;
        Soul randomSoul = souls.get((int) (Math.random() * souls.size()));

        randomSoul.infuseSoul(mob);
        unboundSoulIds.remove(randomSoul.id());
        pdc.set(Soul.UNBOUND_SOULS, PersistentDataType.LIST.strings(), unboundSoulIds);
    }

    @EventHandler
    public void onSoulFreed(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getKiller() == null) return;

        List<Soul> souls = Soul.getCarriedSouls(mob);
        if (souls.isEmpty()) return;

        souls.getFirst().offerSoulReward(mob.getLocation());
    }

    @EventHandler
    public void onClaimSoul(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) return;

        PersistentDataContainer pdc = interaction.getPersistentDataContainer();
        if (!pdc.has(Soul.SOUL_REWARD)) return;

        interaction.remove();
        Stream.of(
                        interaction.getWorld().getNearbyEntitiesByType(TextDisplay.class, interaction.getLocation(), 2),
                        interaction.getWorld().getNearbyEntitiesByType(ItemDisplay.class, interaction.getLocation(), 2)
                )
                .flatMap(Collection::stream)
                .filter(display -> display.getPersistentDataContainer()
                        .getOrDefault(Soul.SOUL_REWARD, PersistentDataType.STRING, "")
                        .equals(interaction.getUniqueId().toString()))
                .forEach(Entity::remove);

        Soul reward = SoulRegistry.getInstance().getSoul(pdc.get(Soul.SOUL_REWARD, PersistentDataType.STRING));
        if (reward == null) {
            SoulSnatcher.getPlugin().getLogger().warning("Could not load soul reward " + pdc.get(Soul.SOUL_REWARD, PersistentDataType.STRING));
            interaction.getWorld().spawnParticle(Particle.EXPLOSION, interaction.getLocation().toCenterLocation(), 1);
            return;
        }

        Player player = event.getPlayer();

        boolean successfulBound = reward.bindSoul(player);
        if (!successfulBound) {
            return;
        }

        SoulEffects.playBindEffect(player, interaction.getLocation().toCenterLocation());
    }

    @EventHandler
    public void onDamageReceivedTrigger(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        List<Soul> souls = Soul.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnDamageReceivedTrigger)
                .map(soul -> (OnDamageReceivedTrigger) soul)
                .forEach(trigger -> {
                    trigger.onDamageReceived(entity, event);
                });
    }

    @EventHandler
    public void onDamageReceivedByEntityTrigger(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!(event.getDamager() instanceof LivingEntity damager)) return;

        List<Soul> souls = Soul.getCarriedSouls(entity);
        souls.stream()
                .filter(soul -> soul instanceof OnDamageReceivedTrigger)
                .map(soul -> (OnDamageReceivedTrigger) soul)
                .forEach(trigger -> {
                    trigger.onDamageReceivedByEntity(entity, damager, event);
                });
    }

}
