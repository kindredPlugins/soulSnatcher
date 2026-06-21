package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.targeting.MonsterGoal;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.items.SoulLanternManager;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SoulType {

    public SoulType() {
    }

    public @NotNull abstract EntityType entityType();

    public abstract @NotNull String id();

    public abstract @NotNull SoulInstance create(LivingEntity carrier);

    public abstract @NotNull SoulCategory category();

    protected abstract @NotNull String skullTexture();

    public abstract @NotNull Component displayName();

    public abstract @NotNull List<Component> description();

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

    public boolean isInvalidInfusionTarget(LivingEntity entity) {
        return entity instanceof Boss || entity instanceof Fish || entity instanceof Bat;
    }

    /**
     * If this soul can be picked up again and replace the old instance, can be used when souls
     * make use of random variables or just to reset soul state, default should be false except
     * if specifically needed
     *
     * @return If this soul can be placed
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
    private static final Map<UUID, List<SoulInstance>> cachedBoundSouls = new LinkedHashMap<>();

    /**
     * Lists all souls connected to the given entity. This works for both infused and bound souls.
     * Souls are retrieved from the cache to save up on memory
     *
     * @return A list of carried souls of the given entity or a Collections empty list
     */
    public static List<SoulInstance> getCarriedSouls(LivingEntity livingEntity) {
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
        List<SoulInstance> cachedSouls = cachedBoundSouls.getOrDefault(livingEntity.getUniqueId(), new ArrayList<>());
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

        if (unboundSouls.size() >= MAX_UNBOUND_SOULS)
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

        if (!(mob instanceof Monster) && (Monster.class.isAssignableFrom(Objects.requireNonNull(entityType().getEntityClass()))))
            Bukkit.getMobGoals().addGoal(mob, 0, new MonsterGoal(mob));
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

    public static final NamespacedKey SOUL_REWARD = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_reward");
    public static final NamespacedKey REWARD_OWNER = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_reward_owner");

    /**
     * Spawns in a set of display entities that are used to enable the player to accept they soul
     * they have been offered by defeating an infused mob
     *
     * @param location The location where to offer the soul, usually the mobs death location
     */
    public void offerSoulReward(Location location, Player owner) {
        location.setPitch(0);

        boolean duplicateSoul = !canOverwriteItself() && getCarriedSouls(owner).stream().anyMatch(soul -> soul.soulType().id().equals(id()));

        location.getWorld().playSound(location, Sound.BLOCK_LEVER_CLICK, 1f, 0.5f);
        location.getWorld().playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 0.5f);

        ItemDisplay skullDisplay = location.getWorld().spawn(location.clone().add(0, 1.5, 0), ItemDisplay.class, display -> {
            display.setItemStack(getRepresentativeSkull());

            Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.PI, 0, 1, 0),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transformation);
            display.setBillboard(Display.Billboard.VERTICAL);
        });
        TextDisplay soulTitle = location.getWorld().spawn(skullDisplay.getLocation().clone().add(0, 0.15, 0), TextDisplay.class, display -> {
            display.text(displayName());
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.VERTICAL);
        });
        TextDisplay interactText = location.getWorld().spawn(skullDisplay.getLocation().clone().add(0, -1, 0), TextDisplay.class, display -> {
            display.text(duplicateSoul ?
                    Component.text("Already bound this soul", NamedTextColor.RED) :
                    Component.keybind("key.use", NamedTextColor.YELLOW).append(Component.text(" to bind")));

            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.VERTICAL);
        });
        Interaction soulInteraction = location.getWorld().spawn(location.clone().add(0, 0.4, 0), Interaction.class, interaction -> {
            interaction.setInteractionHeight(1.5f);
            interaction.setInteractionWidth(1.0f);

            interaction.setVisibleByDefault(false);
            owner.showEntity(SoulSnatcher.getPlugin(), interaction);

            interaction.getPersistentDataContainer().set(REWARD_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
            interaction.getPersistentDataContainer().set(SOUL_REWARD, PersistentDataType.STRING, id());
        });
        List<Entity> displayEntities = List.of(skullDisplay, soulTitle, interactText);
        displayEntities.forEach(entity -> {
            if (!entity.equals(skullDisplay)) {
                entity.setVisibleByDefault(false);
                owner.showEntity(SoulSnatcher.getPlugin(), entity);
            }

            entity.getPersistentDataContainer().set(SOUL_REWARD, PersistentDataType.STRING, soulInteraction.getUniqueId().toString());
            entity.getPersistentDataContainer().set(REWARD_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        });

        long age = duplicateSoul ? 80L : 60 * 20L;

        soulInteraction.getWorld().playSound(soulInteraction.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.25f);
        SoulSnatcher.getPlugin().registerDelayedTask(() -> {
            if (soulInteraction.isDead()) return;

            soulInteraction.remove();
            displayEntities.forEach(Entity::remove);

            SoulEffects.discardSoulRewardEffect(soulInteraction.getLocation());
        }, age);

//        TextDisplay soulDescription = location.getWorld().spawn(skullDisplay.getLocation().clone().add(0, description().size() * -0.75, 0), TextDisplay.class, display -> {
//            display.text(description().stream().reduce((x, y) -> x.appendNewline().append(y.color(NamedTextColor.YELLOW)).color(NamedTextColor.YELLOW)).get());
//            display.setAlignment(TextDisplay.TextAlignment.CENTER);
//            display.setBillboard(Display.Billboard.VERTICAL);
//            display.setShadowed(false);
//            display.setSeeThrough(false);
//        });
    }

    /**
     * Removes a soul reward (meaning all related entities) based on the given interaction entity that is the core of the reward
     *
     * @param rewardTrigger The interaction which acts as core for the reward
     */
    public static void removeSoulReward(Interaction rewardTrigger) {
        rewardTrigger.remove();
        Stream.of(
                        rewardTrigger.getWorld().getNearbyEntitiesByType(TextDisplay.class, rewardTrigger.getLocation(), 2),
                        rewardTrigger.getWorld().getNearbyEntitiesByType(ItemDisplay.class, rewardTrigger.getLocation(), 2)
                )
                .flatMap(Collection::stream)
                .filter(display -> display.getPersistentDataContainer()
                        .getOrDefault(SoulType.SOUL_REWARD, PersistentDataType.STRING, "")
                        .equals(rewardTrigger.getUniqueId().toString()))
                .forEach(Entity::remove);
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
        List<SoulInstance> boundSouls = cachedBoundSouls.getOrDefault(player.getUniqueId(), new ArrayList<>());

        boolean sizeLimitReached = boundSouls.size() >= MAX_BOUND_SOULS;
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
        List<SoulInstance> boundSouls = cachedBoundSouls.getOrDefault(livingEntity.getUniqueId(), Collections.emptyList());
        if (boundSouls.isEmpty()) return false;

        var potSoul = boundSouls.stream().filter(soul -> soul.soulType().id().equals(id())).findFirst();
        if (potSoul.isEmpty()) return false;

        SoulInstance removedSoul = potSoul.get();
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
     *
     * @param mob
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

        clearSouls(player);

        if (!boundSouls.isEmpty()) {
            List<SoulType> souls = boundSouls.stream().map(soulRegistry::getSoul).toList();
            souls.forEach(soulType -> {
                soulType.bindSoul(player);
            });
        }

        if (!unBoundSouls.isEmpty()) {
            List<SoulType> floatingSouls = unBoundSouls.stream().map(soulRegistry::getSoul).toList();
            floatingSouls.forEach(soulType -> {
                soulType.addUnboundSoul(player);
            });
        }
    }
}
