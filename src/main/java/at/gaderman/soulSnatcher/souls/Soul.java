package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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

    /**
     * Infuses the given mob with this soul, this CANNOT be a player! Players have their own way of using souls, infusion is different.
     * This will give the mob a soul-related ability, including custom AI
     * @param mob The mob to be infused with this soul, CANNOT be a player!
     */
    public void infuseSoul(Mob mob){
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        ArrayList<String> boundSouls = new ArrayList<>(pdc.getOrDefault(BOUND_SOULS, PersistentDataType.LIST.strings(), new ArrayList<>()));
        boundSouls.add(id());
        pdc.set(BOUND_SOULS, PersistentDataType.LIST.strings(), boundSouls);

        List<Soul> cachedSouls = cachedBoundSouls.computeIfAbsent(mob.getUniqueId(), (uuid) -> new ArrayList<>());
        cachedSouls.add(this);
        cachedBoundSouls.put(mob.getUniqueId(), cachedSouls);

        SoulEffects.startSoulOrbit(mob, this);
    }
}
