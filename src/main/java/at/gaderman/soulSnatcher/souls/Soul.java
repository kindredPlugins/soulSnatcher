package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public abstract class Soul {

    public abstract String id();
    public abstract EntityType entityType();

    public final ItemStack getRepresentativeSkull() {
        return ItemUtils.createCustomHead("http://textures.minecraft.net/texture/" + skullTexture());
    }

    protected abstract String skullTexture();

    protected abstract Component displayName();

    protected abstract List<Component> description();

    public final ItemStack itemRepresentation() {
        ItemStack item = getRepresentativeSkull();
        item.editMeta(meta -> {
                    meta.itemName(displayName());
                    meta.lore(description());
                }
        );
        return item;
    }

    protected final boolean isPlayerBound(LivingEntity carrier){
        return carrier instanceof Player;
    }

    protected final boolean isInfused(LivingEntity carrier){
        return !(carrier instanceof Player);
    }

    public static final int MAX_BOUND_SOULS = 2;
    public static final int MAX_UNBOUND_SOULS = 15;
    public static final String NO_SOUL_RELEASE_TAG = "no_soul_release";

    public static final NamespacedKey UNBOUND_SOULS = new NamespacedKey(SoulSnatcher.getPlugin(), "unbound_souls");
    public static final NamespacedKey BOUND_SOULS = new NamespacedKey(SoulSnatcher.getPlugin(), "infused_soul");

    private static final Map<UUID, List<Soul>> cachedBoundSouls = new LinkedHashMap<>();

    /**
     * Lists all souls connected to the given entity. This works for both infused and bound souls as they.
     * Souls are retrieved from the cache to save up on memory
     * @param livingEntity
     * @return A list of carried souls of the given entity or a Collections empty list
     */
    public static List<Soul> getCarriedSouls(LivingEntity livingEntity){
        return cachedBoundSouls.getOrDefault(livingEntity.getUniqueId(), Collections.emptyList());
    }

    /**
     * Used when a natural entity is killed, this will play the soul release animation and bind it to the player.
     * This makes it available to the pool so newly spawned mobs can be infused with it.
     * @param location The location for the soul to be release so the location of the killed mob normally
     * @param player The player who killed the mob, this adds the soul into their unbound Soul collection
     */
    public void releaseSoul(Location location, Player player){
        location.setPitch(0);

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        ArrayList<String> unboundSouls = new ArrayList<>(pdc.getOrDefault(UNBOUND_SOULS, PersistentDataType.LIST.strings(), new ArrayList<>()));

        if(unboundSouls.size() >= MAX_UNBOUND_SOULS)
            return;

        unboundSouls.add(id());

        pdc.set(UNBOUND_SOULS, PersistentDataType.LIST.strings(), unboundSouls);

        location.getWorld().playSound(location, Sound.BLOCK_SOUL_SAND_PLACE, 1f, 0.25f);
        location.getWorld().spawnParticle(Particle.SOUL, location, 30, 0.2, 0.5, 0.2, 0.1);

        location.getWorld().spawn(location.clone().add(0, 0.75, 0), ItemDisplay.class, display -> {
            display.setItemStack(getRepresentativeSkull());
            display.setInterpolationDelay(-1);
            display.setInterpolationDuration(40);

            Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
                display.remove();
                display.getWorld().spawnParticle(Particle.WHITE_SMOKE, display.getLocation(), 10, 0.2, 0.2, 0.2, 0);
            }, 40L);
        });
    }

    private void addSoulToPdc(LivingEntity livingEntity){
        PersistentDataContainer pdc = livingEntity.getPersistentDataContainer();
        ArrayList<String> boundSouls = new ArrayList<>(pdc.getOrDefault(BOUND_SOULS, PersistentDataType.LIST.strings(), new ArrayList<>()));
        boundSouls.add(id());
        pdc.set(BOUND_SOULS, PersistentDataType.LIST.strings(), boundSouls);
    }

    /**
     * Infuses the given mob with this soul, this CANNOT be a player! Players have their own way of using souls, infusion is different.
     * This will give the mob a soul-related ability, including custom AI
     * @param mob The mob to be infused with this soul, CANNOT be a player!
     */
    public void infuseSoul(Mob mob){
        addSoulToPdc(mob);

        List<Soul> cachedSouls = cachedBoundSouls.computeIfAbsent(mob.getUniqueId(), (uuid) -> new ArrayList<>());
        cachedSouls.add(this);
        cachedBoundSouls.put(mob.getUniqueId(), cachedSouls);

        SoulEffects.startSoulOrbit(mob, this);
    }

    public static final NamespacedKey SOUL_REWARD = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_reward");

    /**
     * Spawns in a set of display entities that are used to enable the player to accept they soul
     * they have been offered by defeating an infused mob
     * @param location The location where to offer the soul, usually the mobs death location
     */
    public void offerSoulReward(Location location){
        location.setPitch(0);

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
            display.text(Component.keybind("key.use", NamedTextColor.YELLOW).append(Component.text(" to bind")));
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.VERTICAL);
        });
        Interaction soulInteraction = location.getWorld().spawn(location.clone().add(0, 0.4, 0), Interaction.class, interaction -> {
            interaction.setInteractionHeight(1.5f);
            interaction.setInteractionWidth(1.0f);

            interaction.getPersistentDataContainer().set(SOUL_REWARD, PersistentDataType.STRING, id());
        });
        List<Entity> displayEntities = List.of(skullDisplay, soulTitle, interactText);
        displayEntities.forEach(entity -> entity.getPersistentDataContainer().set(SOUL_REWARD, PersistentDataType.STRING, soulInteraction.getUniqueId().toString()));

//        TextDisplay soulDescription = location.getWorld().spawn(skullDisplay.getLocation().clone().add(0, description().size() * -0.75, 0), TextDisplay.class, display -> {
//            display.text(description().stream().reduce((x, y) -> x.appendNewline().append(y.color(NamedTextColor.YELLOW)).color(NamedTextColor.YELLOW)).get());
//            display.setAlignment(TextDisplay.TextAlignment.CENTER);
//            display.setBillboard(Display.Billboard.VERTICAL);
//            display.setShadowed(false);
//            display.setSeeThrough(false);
//        });
    }

    /**
     * Makes the given player bind to this soul. This enables them to use abilities of that soul.
     * Players can only bind with up to 2 souls, if that limit is reached this method will fail
     * and return false.
     * @param player The player to be infused with this soul
     * @return If the soul binding was successful, if the player has already reached their limit of bound souls
     * or already bound with this soul, false will be returned
     */
    public boolean bindSoul(Player player){
        List<Soul> boundSouls = cachedBoundSouls.getOrDefault(player.getUniqueId(), new ArrayList<>());
        if(boundSouls.size() >= MAX_BOUND_SOULS || boundSouls.contains(this)) return false;

        addSoulToPdc(player);
        boundSouls.add(this);
        cachedBoundSouls.put(player.getUniqueId(), boundSouls);

        SoulEffects.startSoulOrbit(player, this);
        return true;
    }

    /**
     * Removes all cached soul entries of the given entity. The cache is only used for quick lookups.
     * Using this helps avoid memory leaks. Never call this unprecedented! This may cause issues!
     * @param livingEntity The entity whose soul data should be removed from the cache
     */
    public static void removeFromCache(LivingEntity livingEntity){
        cachedBoundSouls.remove(livingEntity.getUniqueId());
    }

    /**
     * Clears all soul information of the player. This usually happens on death.
     * @param player The player whose soul data to reset
     */
    public static void clearSouls(Player player){
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.remove(UNBOUND_SOULS);
        pdc.remove(BOUND_SOULS);

        removeFromCache(player);
    }

    /**
     * Loads all soul data stored in the players pdc into cached lists to speed up processing.
     * Usually gets called on login.
     * @param player The player whose soul data gets loaded into cache from pdc
     */
    public static void loadIntoCache(Player player){
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        ArrayList<String> boundSouls = new ArrayList<>(pdc.getOrDefault(BOUND_SOULS, PersistentDataType.LIST.strings(), Collections.emptyList()));
        if(boundSouls.isEmpty()) return;

        SoulRegistry soulRegistry = SoulRegistry.getInstance();
        List<Soul> souls = boundSouls.stream().map(soulRegistry::getSoul).toList();
        souls.forEach(soul -> soul.bindSoul(player));
    }
}
