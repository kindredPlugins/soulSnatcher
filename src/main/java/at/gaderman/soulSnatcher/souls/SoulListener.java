package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Stream;

public class SoulListener implements Listener {

    @EventHandler(ignoreCancelled = true,priority = EventPriority.HIGH)
    public void onSoulRelease(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getKiller() == null) return;
        if (mob.getScoreboardTags().contains(Soul.NO_SOUL_RELEASE_TAG)) return;

        var optSoul = SoulRegistry.getInstance().getSoul(mob.getType());
        if (optSoul.isEmpty()) return;
        var souls = Soul.getCarriedSouls(mob);
         if(!souls.isEmpty()) return;

        optSoul.get().releaseSoul(mob.getLocation(), mob.getKiller());
    }

    @EventHandler
    public void onSoulInfuse(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if(event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;

        Collection<Player> nearbyPlayers = mob.getWorld().getNearbyPlayers(mob.getLocation(), 50, 50);
        if (nearbyPlayers.isEmpty()) return;

        Player closestPlayer = nearbyPlayers
                .stream()
                .min(Comparator.comparing(player -> player.getLocation().distanceSquared(mob.getLocation())))
                .get();

        List<Soul> unboundSouls = Soul.getUnboundSouls(closestPlayer);

        if (unboundSouls.isEmpty()) return;
        Soul randomSoul = unboundSouls.get((int) (Math.random() * unboundSouls.size()));

        randomSoul.infuseSoul(mob);
        randomSoul.removeUnboundSoul(closestPlayer);
    }

    @EventHandler
    public void onSoulFreed(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getKiller() == null) return;

        List<Soul> souls = Soul.getCarriedSouls(mob);
        if (souls.isEmpty()) return;

        souls.getFirst().offerSoulReward(mob.getLocation(), mob.getKiller());
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
            //TODO: display GUI that allows player to replace an existing soul with the reward
            return;
        }

        SoulEffects.playBindEffect(player, interaction.getLocation().toCenterLocation());
    }

    /**
     * Entities who die have their soul removed from the cache. Players who die lose all soul data
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event){
       if(event.getEntity() instanceof Player player)
           Soul.clearSouls(player);

       else Soul.removeFromCache(event.getEntity());
    }

    /**
     * When a player joins, their data needs to be loaded into the cache so it can be processed quickly
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Soul.loadIntoCache(event.getPlayer());
    }

    /**
     * When a player quits, he should be removed from soul cache to avoid memory leak
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Soul.removeFromCache(event.getPlayer());
    }

}
