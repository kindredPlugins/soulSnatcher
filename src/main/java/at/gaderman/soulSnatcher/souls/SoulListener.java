package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.gui.interaction.SoulAbsorptionUI;
import at.gaderman.soulSnatcher.souls.config.OfflineUnboundPoolConfig;
import at.gaderman.soulSnatcher.souls.effects.SoulEffects;
import at.gaderman.soulSnatcher.souls.effects.SoulReward;
import at.gaderman.soulSnatcher.souls.items.SoulLanternManager;
import at.gaderman.soulSnatcher.souls.items.SoulVialManager;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Stream;

public class SoulListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSoulRelease(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getKiller() == null) return;
        if (mob.getScoreboardTags().contains(SoulType.NO_SOUL_RELEASE_TAG)) return;
        if (mob.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) return;

        var optSoul = SoulRegistry.getInstance().getSoul(mob.getType());
        if (optSoul.isEmpty()) return;
        var souls = SoulType.getCarriedSouls(mob);
        if (!souls.isEmpty()) return;

        if(mob instanceof Boss){
            SoulReward.offerSoulReward(mob.getLocation(), mob.getKiller(), optSoul.get());
            return;
        }

        optSoul.get().releaseSoul(mob.getLocation(), mob.getKiller());
    }

    public static NamespacedKey INFUSED_FROM = new NamespacedKey(SoulSnatcher.getPlugin(), "infused_from");

    @EventHandler
    public void onSoulInfuse(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.RAID)
            return;

        boolean isRaidSpawn = event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.RAID;
        double xzRadius = isRaidSpawn ? 300 : 50;
        double yRadius = isRaidSpawn ? 300 : 30;
        Collection<Player> nearbyPlayers = mob.getWorld().getNearbyPlayers(mob.getLocation(), xzRadius, yRadius);
        if (nearbyPlayers.isEmpty()) return;

        Player closestPlayer = nearbyPlayers
                .stream()
                .min(Comparator.comparing(player -> player.getLocation().distanceSquared(mob.getLocation())))
                .get();

        List<SoulType> unboundSouls = new ArrayList<>(SoulType.getUnboundSouls(closestPlayer));

        SoulType randomSoul = null;
        boolean foundValidSoul = false;
        while (!unboundSouls.isEmpty()) {
            randomSoul = unboundSouls.remove((int) (Math.random() * unboundSouls.size()));

            if (randomSoul.isInvalidInfusionTarget(mob)) continue;
            if (randomSoul.entityType().equals(mob.getType())) continue;

            foundValidSoul = true;
            break;
        }

        if (!foundValidSoul)
            return;

        randomSoul.infuseSoul(mob);
        randomSoul.removeUnboundSoul(closestPlayer);

        mob.getPersistentDataContainer().set(INFUSED_FROM, PersistentDataType.STRING, closestPlayer.getUniqueId().toString());
    }

    @EventHandler
    public void onSoulFreed(EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.getKiller() == null) return;

        List<SoulInstance<?>> souls = SoulType.getCarriedSouls(livingEntity);
        if (souls.isEmpty()) return;

        Location origin = livingEntity.getLocation();
        for (int i = 0; i < souls.size(); i++) {
            SoulInstance<?> soul = souls.get(i);
            SoulReward.offerSoulReward(origin.add(0, i * 1.5, 0), livingEntity.getKiller(), soul.soulType());
        }
    }

    @EventHandler
    public void onInfuseNaturalDespawn(EntityRemoveEvent event) {
        if (event.getCause() != EntityRemoveEvent.Cause.DESPAWN)
            return;

        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(INFUSED_FROM)) return;

        var souls = SoulType.getCarriedSouls(entity);
        if (souls.isEmpty()) return;

        String uuid = pdc.getOrDefault(INFUSED_FROM, PersistentDataType.STRING, UUID.randomUUID().toString());
        Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        if (player == null) {
            OfflineUnboundPoolConfig poolConfig = OfflineUnboundPoolConfig.getInstance();
            souls.forEach(soul -> poolConfig.addToOfflinePoolPlayer(uuid, soul.soulType()));
            return;
        }

        souls.forEach(soul -> soul.soulType().releaseSoul(entity.getLocation(), player));
    }

    @EventHandler
    public void onClaimSoul(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) return;

        PersistentDataContainer pdc = interaction.getPersistentDataContainer();
        if (!pdc.has(SoulReward.SOUL_REWARD)) return;

        Player player = event.getPlayer();

        SoulType reward = SoulRegistry.getInstance().getSoul(pdc.get(SoulReward.SOUL_REWARD, PersistentDataType.STRING));
        if (reward == null) {
            SoulSnatcher.getPlugin().getLogger().warning("Could not load soul reward " + pdc.get(SoulReward.SOUL_REWARD, PersistentDataType.STRING));
            interaction.getWorld().spawnParticle(Particle.EXPLOSION, interaction.getLocation().toCenterLocation(), 1);
            return;
        }

        if (SoulVialManager.checkAndFillVialIfPresent(player, reward, interaction.getLocation())) {
            SoulReward.removeSoulReward(interaction);
            return;
        }

        boolean successfulBound = reward.bindSoul(player);
        if (!successfulBound) {
            if (SoulType.getCarriedSouls(player).stream().anyMatch(soul -> soul.soulType().equals(reward))) return;

            new SoulAbsorptionUI(player, reward, interaction).openInventory(player);
            return;
        }

        SoulReward.removeSoulReward(interaction);
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
    public void onSoulMobTransformation(EntityTransformEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getTransformedEntity() instanceof Mob transformed))
            return;

        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        if (!pdc.has(SoulType.BOUND_SOULS, PersistentDataType.LIST.strings())) return;
        var x = SoulType.getCarriedSouls(mob);
        if (SoulType.getCarriedSouls(mob).isEmpty()) return;

        SoulType.getCarriedSouls(mob).forEach(soul -> soul.soulType().infuseSoul(transformed));
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
        Player player = event.getPlayer();
        SoulType.loadIntoCache(player);
        loadSouls(player);
    }

    /**
     * When a player quits, he should be removed from soul cache to avoid memory leak
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        SoulType.removeFromCache(event.getPlayer());
        event.getPlayer().saveData();
    }

    @EventHandler
    public void onPlayerTeleportCrossWorlds(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() == event.getTo().getWorld())
            return;

        Player player = event.getPlayer();
        loadSouls(player);

        Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
            List<SoulInstance<?>> soulTypes = SoulType.getCarriedSouls(player);
            SoulEffects.stopAllSoulOrbits(player);
            soulTypes.forEach(soul -> SoulEffects.addSoulToOrbit(player, soul.soulType()));

            if (SoulLanternManager.isLookingAtOrbits(player))
                SoulEffects.showSoulOrbits(player);
        }, 1L);
    }

    private void loadSouls(Player player) {
        Stream.of(
                        player.getWorld().getEntitiesByClass(TextDisplay.class),
                        player.getWorld().getEntitiesByClass(ItemDisplay.class),
                        player.getWorld().getEntitiesByClass(Interaction.class)
                )
                .flatMap(Collection::stream)
                .filter(display -> display.getPersistentDataContainer()
                        .getOrDefault(SoulReward.REWARD_OWNER, PersistentDataType.STRING, "").equals(player.getUniqueId().toString()))
                .forEach(display -> {
                    if (display.getPersistentDataContainer().has(SoulReward.HIDDEN_FOR))
                        player.hideEntity(SoulSnatcher.getPlugin(), display);

                    else player.showEntity(SoulSnatcher.getPlugin(), display);
                });
    }

    @EventHandler
    public void onExpiredSoulLoad(EntityAddToWorldEvent event) {
        if(!(event.getEntity() instanceof Display display))
            return;

        PersistentDataContainer pdc = display.getPersistentDataContainer();
        if(!pdc.has(SoulReward.SOUL_REWARD))
            return;

        long timeStamp = pdc.getOrDefault(SoulReward.TIMESTAMP, PersistentDataType.LONG, 0L);

        if(timeStamp >= System.currentTimeMillis() - SoulReward.LIVING_TICKS * 50)
            return;

        display.getScheduler().runDelayed(SoulSnatcher.getPlugin(), _ -> display.remove(), null, 1L);
    }
}
