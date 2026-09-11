package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.config.GeneralConfig;
import at.gaderman.soulSnatcher.config.lang.LanguageKeyHolder;
import at.gaderman.soulSnatcher.config.lang.LanguageManager;
import at.gaderman.soulSnatcher.souls.config.OfflineUnboundPoolConfig;
import at.gaderman.soulSnatcher.souls.effects.SoulEffects;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.items.SoulLanternManager;
import at.gaderman.soulSnatcher.souls.items.SoulVialManager;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import io.papermc.paper.entity.PlayerGiveResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class SoulType implements LanguageKeyHolder {

    public SoulType() {
    }

    public @NotNull
    abstract EntityType entityType();

    public abstract @NotNull String id();

    public abstract @NotNull SoulInstance<?> create(LivingEntity carrier);

    public abstract @NotNull SoulCategory category();

    protected abstract @NotNull String skullTexture();

    public abstract @NotNull Component defaultDisplayName();

    public abstract @NotNull List<Component> defaultDescription();

    public final @NotNull ItemStack getRepresentativeSkull() {
        return ItemUtils.createCustomHead("http://textures.minecraft.net/texture/" + skullTexture());
    }

    public final @NotNull ItemStack itemRepresentation() {
        ItemStack item = getRepresentativeSkull();
        item.editMeta(meta -> {
                    meta.displayName(displayName().decoration(TextDecoration.ITALIC, false));
                    meta.lore(description());
                }
        );
        return item;
    }

    private static final String NAMES_LANG_PREFIX = "soul_names.";
    private static final String DESCRIPTION_LANG_PREFIX = "soul_descriptions.";

    public @NotNull Component displayName() {
        Component displayName = LanguageManager.getInstance().resolveComponent(NAMES_LANG_PREFIX + id()).getFirst();

        return SoulLanguageDefinitions.SOUL_NAME.getSingle()
                .style(displayName.style())
                .colorIfAbsent(displayFallbackColor())
                .replaceText(TextReplacementConfig.builder()
                        .matchLiteral(SoulLanguageDefinitions.SOUL_PLACEHOLDER)
                        .replacement(displayName)
                        .build());
    }

    protected TextColor displayFallbackColor() {
        return NamedTextColor.GRAY;
    }

    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(LanguageManager.getInstance().resolveComponent(DESCRIPTION_LANG_PREFIX + id()));
    }

    @Override
    public Map<String, List<Component>> languageKeyDefaultMap() {
        return Map.of(
                NAMES_LANG_PREFIX + id(), List.of(defaultDisplayName()),
                DESCRIPTION_LANG_PREFIX + id(), defaultDescription()
        );
    }

    public boolean isInvalidInfusionTarget(LivingEntity entity) {
        return entity instanceof Boss || entity instanceof Fish || entity instanceof Bat;
    }

    /**
     * If this soul can be picked up again and replace the old instance, can be used when souls
     * make use of random variables or just to reset soul state, default should be false except
     * if specifically needed
     *
     * @return If this soul can be obtained again while carrying allowing it to overwrite itself
     * @see at.gaderman.soulSnatcher.souls.instances.attributes.HorseSoulType
     */
    public boolean canOverwriteItself() {
        return false;
    }

    public static final int MAX_BOUND_SOULS = 2;
    public static final int MAX_UNBOUND_SOULS = 15;
    public static final String NO_SOUL_RELEASE_TAG = "no_soul_release";

    public static final NamespacedKey UNBOUND_SOULS = new NamespacedKey(SoulSnatcher.getPlugin(), "unbound_souls");
    public static final NamespacedKey BOUND_SOULS = new NamespacedKey(SoulSnatcher.getPlugin(), "infused_soul");

    private static final Map<UUID, List<SoulType>> cachedUnboundSouls = new LinkedHashMap<>();
    private static final Map<UUID, List<SoulInstance<?>>> cachedBoundSouls = new LinkedHashMap<>();

    /**
     * Lists all souls connected to the given entity. This works for both infused and bound souls.
     * Souls are retrieved from the cache to save up on memory
     *
     * @return A list of carried souls of the given entity or a Collections empty list
     */
    public static List<SoulInstance<?>> getCarriedSouls(LivingEntity livingEntity) {
        return cachedBoundSouls.getOrDefault(livingEntity.getUniqueId(), Collections.emptyList());
    }

    /**
     * Lists all released unbound souls "hanging" on the player. These are used to infuse nearby
     * spawning mobs
     *
     * @param player
     * @return A list of all unbound souls of the given player yet to be infused with a mob
     */
    public static List<SoulType> getUnboundSouls(Player player) {
        return cachedUnboundSouls.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    private void addSoul(LivingEntity livingEntity) {
        List<SoulInstance<?>> cachedSouls = cachedBoundSouls.getOrDefault(livingEntity.getUniqueId(), new ArrayList<>());
        cachedSouls.add(create(livingEntity));
        cachedBoundSouls.put(livingEntity.getUniqueId(), cachedSouls);
    }

    private void addUnboundSoul(Player player) {
        List<SoulType> cachedUnboundSoulList = cachedUnboundSouls.getOrDefault(player.getUniqueId(), new ArrayList<>());
        cachedUnboundSoulList.add(this);
        cachedUnboundSouls.put(player.getUniqueId(), cachedUnboundSoulList);
    }

    private void addSoulToPdc(LivingEntity livingEntity) {
        PersistentDataContainer pdc = livingEntity.getPersistentDataContainer();
        ArrayList<String> boundSouls = new ArrayList<>(pdc.getOrDefault(BOUND_SOULS, PersistentDataType.LIST.strings(), new ArrayList<>()));
        boundSouls.add(id());
        pdc.set(BOUND_SOULS, PersistentDataType.LIST.strings(), boundSouls);
    }

    /**
     * Used when a natural entity is killed, this will play the soul release animation and bind it to the player.
     * This makes it available to the pool so newly spawned mobs can be infused with it.
     *
     * @param location The location for the soul to be release so the location of the killed mob normally
     * @param player   The player who killed the mob, this adds the soul into their unbound Soul collection
     */
    public void releaseSoul(Location location, Player player) {
        location.setPitch(0);

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        ArrayList<String> unboundSouls = new ArrayList<>(pdc.getOrDefault(UNBOUND_SOULS, PersistentDataType.LIST.strings(), new ArrayList<>()));

        if (unboundSouls.size() >= GeneralConfig.getInstance().MAX_UNBOUND_SOULS.cached())
            return;

        unboundSouls.add(id());
        pdc.set(UNBOUND_SOULS, PersistentDataType.LIST.strings(), unboundSouls);

        addUnboundSoul(player);

        SoulEffects.spawnReleasedSoul(location, itemRepresentation());
    }

    /**
     * Infuses the given mob with this soul, this CANNOT be a player! Players have their own way of using souls, infusion is different.
     * This will give the mob a soul-related ability, including custom AI
     *
     * @param mob The mob to be infused with this soul, CANNOT be a player!
     */
    public void infuseSoul(Mob mob) {
        addSoulToPdc(mob);
        addSoul(mob);

        SoulEffects.addSoulToOrbit(mob, this);

//        if (!(mob instanceof Monster) && (Monster.class.isAssignableFrom(Objects.requireNonNull(entityType().getEntityClass()))))
//            Bukkit.getMobGoals().addGoal(mob, 0, new MonsterGoal(mob));
    }

    /**
     * Correctly removes an unbound soul from the associated player, both in terms of cache and pdc.
     *
     * @param player The player to have one instance of this soul removed from their unbound soul pool.
     */
    public void removeUnboundSoul(Player player) {
        List<SoulType> unboundSouls = cachedUnboundSouls.getOrDefault(player.getUniqueId(), Collections.emptyList());
        if (unboundSouls.isEmpty()) return;

        unboundSouls.remove(this);
        cachedUnboundSouls.put(player.getUniqueId(), unboundSouls);

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        List<String> unboundSoulIds = new ArrayList<>(pdc.getOrDefault(UNBOUND_SOULS, PersistentDataType.LIST.strings(), List.of()));
        unboundSoulIds.remove(id());
        pdc.set(UNBOUND_SOULS, PersistentDataType.LIST.strings(), unboundSoulIds);
    }

    /**
     * Makes the given player bind to this soul. This enables them to use abilities of that soul.
     * Players can only bind with up to 2 souls, if that limit is reached this method will fail
     * and return false.
     *
     * @param player The player to be infused with this soul
     * @return If the soul binding was successful, if the player has already reached their limit of bound souls
     * or already bound with this soul, false will be returned
     */
    public boolean bindSoul(Player player) {
        List<SoulInstance<?>> boundSouls = cachedBoundSouls.getOrDefault(player.getUniqueId(), new ArrayList<>());

        boolean sizeLimitReached = boundSouls.size() >= GeneralConfig.getInstance().MAX_BOUND_SOULS.cached();
        if (!canOverwriteItself() && sizeLimitReached) return false;

        boolean isDuplicate = boundSouls.stream().anyMatch(soul -> soul.soulType().equals(this));

        if (isDuplicate) {
            if (!canOverwriteItself())
                return false;

            boundSouls.stream().toList().stream()
                    .filter(soul -> soul.soulType().equals(this))
                    .forEach(soulInstance -> {
                        soulInstance.soulType().removeSoul(player);
                    });

        } else if (sizeLimitReached) {
            return false;
        }

        addSoulToPdc(player);
        boundSouls.add(create(player));
        cachedBoundSouls.put(player.getUniqueId(), boundSouls);

        SoulLanternManager.updateActiveLanterns(player);
        SoulEffects.addSoulToOrbit(player, this);
        return true;
    }

    /**
     * Removes the soul with this soulType from the given target
     *
     * @param livingEntity The target who should have this soulType removed from them
     * @return true if a soul of this type was removed successfully, otherwise false
     */
    public boolean removeSoul(LivingEntity livingEntity) {
        List<SoulInstance<?>> boundSouls = cachedBoundSouls.getOrDefault(livingEntity.getUniqueId(), Collections.emptyList());
        if (boundSouls.isEmpty()) return false;

        var potSoul = boundSouls.stream().filter(soul -> soul.soulType().id().equals(id())).findFirst();
        if (potSoul.isEmpty()) return false;

        SoulInstance<?> removedSoul = potSoul.get();
        boundSouls.remove(removedSoul);
        PersistentDataContainer pdc = livingEntity.getPersistentDataContainer();

        if (boundSouls.isEmpty()) {
            cachedBoundSouls.remove(livingEntity.getUniqueId());
            pdc.remove(BOUND_SOULS);
        } else {
            cachedBoundSouls.put(livingEntity.getUniqueId(), boundSouls);
            pdc.set(BOUND_SOULS, PersistentDataType.LIST.strings(), boundSouls.stream()
                    .map(soul -> soul.soulType().id()).collect(Collectors.toList()));
        }

        if (livingEntity instanceof Player player)
            SoulLanternManager.updateActiveLanterns(player);
        removedSoul.reset();
        SoulEffects.removeOneSoulFromOrbit(livingEntity, this);
        return true;
    }

    /**
     * Removes all cached soul entries of the given entity. The cache is only used for quick lookups.
     * Using this helps avoid memory leaks. Never call this unprecedented! This may cause issues!
     *
     * @param livingEntity The entity whose soul data should be removed from the cache
     */
    public static void removeFromCache(LivingEntity livingEntity) {
        cachedBoundSouls.getOrDefault(livingEntity.getUniqueId(), Collections.emptyList()).forEach(SoulInstance::cleanUp);

        cachedBoundSouls.remove(livingEntity.getUniqueId());
        if (livingEntity instanceof Player)
            cachedUnboundSouls.remove(livingEntity.getUniqueId());
    }

    /**
     * Clears all soul information of the player. This usually happens on death.
     *
     * @param player The player whose soul data to reset
     */
    public static void clearSouls(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.remove(UNBOUND_SOULS);
        pdc.remove(BOUND_SOULS);

        cachedBoundSouls.getOrDefault(player.getUniqueId(), Collections.emptyList()).forEach(SoulInstance::reset);
        removeFromCache(player);
    }

    /**
     * Loads all soul data stored in the pdc of a mob into cache and sets up all infusions.
     */
    public static void loadIntoCache(Mob mob) {
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        ArrayList<String> infusedSoulIds = new ArrayList<>(pdc.getOrDefault(BOUND_SOULS, PersistentDataType.LIST.strings(), Collections.emptyList()));

        pdc.remove(BOUND_SOULS);

        SoulRegistry registry = SoulRegistry.getInstance();
        infusedSoulIds.stream()
                .map(registry::getSoul)
                .filter(Objects::nonNull)
                .forEach(soulType -> soulType.infuseSoul(mob));
    }

    /**
     * Loads all soul data stored in the players pdc into cached lists to speed up processing.
     * Usually gets called on login.
     *
     * @param player The player whose soul data gets loaded into cache from pdc
     */
    public static void loadIntoCache(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        ArrayList<String> boundSouls = new ArrayList<>(pdc.getOrDefault(BOUND_SOULS, PersistentDataType.LIST.strings(), Collections.emptyList()));
        ArrayList<String> unBoundSouls = new ArrayList<>(pdc.getOrDefault(UNBOUND_SOULS, PersistentDataType.LIST.strings(), Collections.emptyList()));
        SoulRegistry soulRegistry = SoulRegistry.getInstance();

        OfflineUnboundPoolConfig poolConfig = OfflineUnboundPoolConfig.getInstance();
        unBoundSouls.addAll(poolConfig.offlinePoolOfPlayer(player));
        poolConfig.clearOfflinePoolPlayer(player);

        clearSouls(player);

        if (!boundSouls.isEmpty()) {
            List<SoulType> souls = boundSouls.stream()
                    .map(soulRegistry::getSoul)
                    .filter(Objects::nonNull)
                    .toList();
            souls.forEach(soulType -> {
                boolean bound = soulType.bindSoul(player);

                if (!bound) {
                    PlayerGiveResult giveResult = player.give(List.of(SoulVialManager.getFilledVial(soulType)), true);
                    giveResult.drops().forEach(item -> {
                        item.setOwner(player.getUniqueId());
                        item.setGlowing(true);
                        item.setInvulnerable(true);
                    });
                }
            });

            List<SoulType> legacySouls = boundSouls.stream()
                    .map(legacyId -> soulRegistry.legacySoulRegistryMap().getOrDefault(legacyId, null))
                    .filter(Objects::nonNull)
                    .toList();
            AtomicBoolean hadDrops = new AtomicBoolean(false);
            legacySouls.forEach(soulType -> {
                ItemStack filledVial = SoulVialManager.getFilledVial(soulType);

                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItem(player.getLocation(), filledVial, drop -> {
                        drop.setOwner(player.getUniqueId());
                        drop.setGlowing(true);
                        drop.setHealth(100);
                        drop.setVelocity(drop.getVelocity().multiply(0));
                    });
                    hadDrops.set(true);
                } else {
                    player.give(filledVial);
                }

                boundSouls.remove(soulType.id());
            });

            if (!legacySouls.isEmpty()) {
                Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("------------- ", NamedTextColor.GRAY)
                            .append(Component.text("SoulSnatcher", NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true))
                            .append(Component.text(" -------------", NamedTextColor.GRAY)));
                    SoulLanguageDefinitions.DISABLED_WHILE_OFFLINE.getLines().forEach(player::sendMessage);
                    legacySouls.stream()
                            .map(soul -> Component.text("➤ ", NamedTextColor.GRAY)
                                    .append(soul.displayName().decoration(TextDecoration.ITALIC, false)))
                            .toList()
                            .forEach(player::sendMessage);
                    player.sendMessage(Component.empty());
                    SoulLanguageDefinitions.RECEIVED_AS_SOUL_VIAL.getLines()
                            .stream().map(line -> line.replaceText(TextReplacementConfig.builder()
                                    .matchLiteral(SoulLanguageDefinitions.VIAL_PLACEHOLDER)
                                    .replacement(SoulVialManager.getEmptyVial().displayName())
                                    .build()))
                            .forEach(player::sendMessage);
                    if (hadDrops.get()) {
                        SoulLanguageDefinitions.VIAL_DROP_FULL_INV.getLines().forEach(player::sendMessage);
                    }
                    player.sendMessage(Component.empty());

                    player.playSound(player, Sound.ENTITY_WITHER_AMBIENT, 3f, 0.5f);
                }, 20L);

                pdc.set(BOUND_SOULS, PersistentDataType.LIST.strings(), boundSouls);
            }
        }

        if (!unBoundSouls.isEmpty()) {
            List<SoulType> floatingSouls = unBoundSouls.stream()
                    .map(soulRegistry::getSoul)
                    .filter(Objects::nonNull)
                    .toList();
            floatingSouls.forEach(soulType -> {
                soulType.addUnboundSoul(player);
            });
        }
    }
}
