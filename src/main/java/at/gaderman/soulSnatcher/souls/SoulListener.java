package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.gui.interaction.SoulAbsorptionUI;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class SoulListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSoulRelease(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getKiller() == null) return;
        if (mob.getScoreboardTags().contains(SoulType.NO_SOUL_RELEASE_TAG)) return;

        var optSoul = SoulRegistry.getInstance().getSoul(mob.getType());
        if (optSoul.isEmpty()) return;
        var souls = SoulType.getCarriedSouls(mob);
        if (!souls.isEmpty()) return;

        optSoul.get().releaseSoul(mob.getLocation(), mob.getKiller());
    }

    @EventHandler
    public void onSoulInfuse(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)
            return;

        Collection<Player> nearbyPlayers = mob.getWorld().getNearbyPlayers(mob.getLocation(), 50, 50);
        if (nearbyPlayers.isEmpty()) return;

        Player closestPlayer = nearbyPlayers
                .stream()
                .min(Comparator.comparing(player -> player.getLocation().distanceSquared(mob.getLocation())))
                .get();

        List<SoulType> unboundSouls = SoulType.getUnboundSouls(closestPlayer);

        if (unboundSouls.isEmpty()) return;
        SoulType randomSoul = unboundSouls.get((int) (Math.random() * unboundSouls.size()));

        if (randomSoul.entityType().equals(mob.getType())) return;

        randomSoul.infuseSoul(mob);
        randomSoul.removeUnboundSoul(closestPlayer);
    }

    @EventHandler
    public void onSoulFreed(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getKiller() == null) return;

        List<SoulInstance> souls = SoulType.getCarriedSouls(mob);
        if (souls.isEmpty()) return;

        souls.getFirst().soulType().offerSoulReward(mob.getLocation(), mob.getKiller());
    }

    @EventHandler
    public void onClaimSoul(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) return;

        PersistentDataContainer pdc = interaction.getPersistentDataContainer();
        if (!pdc.has(SoulType.SOUL_REWARD)) return;

        Player player = event.getPlayer();

        SoulType reward = SoulRegistry.getInstance().getSoul(pdc.get(SoulType.SOUL_REWARD, PersistentDataType.STRING));
        if (reward == null) {
            SoulSnatcher.getPlugin().getLogger().warning("Could not load soul reward " + pdc.get(SoulType.SOUL_REWARD, PersistentDataType.STRING));
            interaction.getWorld().spawnParticle(Particle.EXPLOSION, interaction.getLocation().toCenterLocation(), 1);
            return;
        }

        boolean successfulBound = reward.bindSoul(player);
        if (!successfulBound) {
            if (SoulType.getCarriedSouls(player).stream().anyMatch(soul -> soul.soulType().equals(reward))) return;

            new SoulAbsorptionUI(player, reward, interaction).openInventory(player);
            return;
        }

        SoulType.removeSoulReward(interaction);
        SoulEffects.playBindEffect(player, reward, interaction.getLocation());
    }

    /**
     * Entities who die have their soul removed from the cache. Players who die lose all soul data
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player)
            SoulType.clearSouls(player);

        else SoulType.removeFromCache(event.getEntity());
    }

    @EventHandler
    public void onInfusedAddedToWorld(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        if (!pdc.has(SoulType.BOUND_SOULS, PersistentDataType.LIST.strings())) return;
        if (!SoulType.getCarriedSouls(mob).isEmpty()) return;

        Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
            if (!mob.isValid()) return;

            mob.getLocation().getWorld().getChunkAtAsync(mob.getLocation())
                    .thenAccept(chunk ->
                            Bukkit.getScheduler().runTask(SoulSnatcher.getPlugin(), () -> {
                                if (!mob.isValid()) return;
                                SoulType.loadIntoCache(mob);
                            })
                    );
        }, 1L);
    }

    @EventHandler
    public void onInfusedRemovedFromWorld(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            SoulType.removeFromCache(mob);
        }
    }

    /**
     * When a player joins, their data needs to be loaded into the cache so it can be processed quickly
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SoulType.loadIntoCache(event.getPlayer());
        loadSouls(event.getPlayer());
    }

    /**
     * When a player quits, he should be removed from soul cache to avoid memory leak
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        SoulType.removeFromCache(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
       loadSouls(event.getPlayer());
    }

    private void loadSouls(Player player){
        Stream.of(
                        player.getWorld().getEntitiesByClass(TextDisplay.class),
                        player.getWorld().getEntitiesByClass(ItemDisplay.class),
                        player.getWorld().getEntitiesByClass(Interaction.class)
                )
                .flatMap(Collection::stream)
                .filter(display -> display.getPersistentDataContainer()
                        .getOrDefault(SoulType.REWARD_OWNER, PersistentDataType.STRING, "").equals(player.getUniqueId().toString()))
                .forEach(display -> player.showEntity(SoulSnatcher.getPlugin(), display));
    }
}
