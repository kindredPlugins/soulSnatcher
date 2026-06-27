package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.targeting.SearchAndAddPassengerGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.OnTargetTrigger;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSneakToggleTrigger;
import at.gaderman.soulSnatcher.souls.triggers.interact.OnPlayerInteractEntityTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.entity.boat.OakBoat;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class PigSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<PigSoulType> create(LivingEntity carrier) {
        return new PigSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "pig_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.PIG;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "9b1760e3778f8087046b86bec6a0a83a567625f30f0d6bce866d4bed95dba6c1";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Pig Soul", TextColor.color(0xeea5a4));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.keybind("key.sneak", NamedTextColor.GOLD)
                        .append(Component.text(" + "))
                        .append(Component.keybind("key.use", NamedTextColor.GOLD))
                        .append(Component.text(" an entity to", NamedTextColor.WHITE)),
                Component.text("saddle them up on yourself."),
                Component.text("(Hold ", NamedTextColor.GRAY)
                        .append(Component.keybind("key.sneak"))
                        .append(Component.text(" to drop them)"))
        );
    }

    //region Config Valfues

    private static final String DROP_PASSENGER_DELAY_CONFIG_ID = "drop_passenger_delay_ticks";
    private final ConfigOption<Integer> dropPassengerDelay = configOption(DROP_PASSENGER_DELAY_CONFIG_ID, 30, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(DROP_PASSENGER_DELAY_CONFIG_ID, "How long players have to sneak for a passenger to be dropped in ticks (20 ticks = 1 second)");
    }

    //endregion

    public static class PigSoulInstance extends SoulInstance<PigSoulType> implements OnPlayerInteractEntityTrigger, OnTargetTrigger, OnSneakToggleTrigger {
        protected PigSoulInstance(LivingEntity carrier, PigSoulType soulType) {
            super(carrier, soulType);

            if (carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new SearchAndAddPassengerGoal(mob));
        }

        private BukkitTask dropPassengerTask;

        private static double boatWidth;

        public static boolean isValidPassengerTarget(Entity target) {
            if (boatWidth == 0) {
                Boat measurementObject = target.getWorld().spawn(new Location(target.getWorld(), 0, -500, 0), OakBoat.class);
                boatWidth = measurementObject.getWidth();
                measurementObject.remove();
            }

            return target.getWidth() <= boatWidth || target instanceof Spider;
        }

        @Override
        public void onPlayerInteractEntity(Player player, Entity entity, PlayerInteractEntityEvent event) {
            if (!player.isSneaking()) return;
            if (!player.getPassengers().isEmpty()) return;
            if (!player.getEquipment().getItemInMainHand().isEmpty()) return;
            if (!isValidPassengerTarget(entity)) return;

            if (!player.addPassenger(entity)) return;

            player.getWorld().playSound(player, Sound.ENTITY_PIG_SADDLE, 1f, 0.5f);
            if (entity instanceof Mob mob && player.equals(mob.getTarget()))
                mob.setTarget(null);
        }

        @Override
        public void onSneakToggle(Player carrier, PlayerToggleSneakEvent event) {
            if (event.isSneaking() && dropPassengerTask == null) {
                dropPassengerTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        dropPassengerTask = null;

                        carrier.eject();
                        carrier.getWorld().playSound(carrier, Sound.ITEM_SADDLE_UNEQUIP, 1f, 1f);
                    }
                }.runTaskLater(SoulSnatcher.getPlugin(), soulType().dropPassengerDelay.cached());
            }

            if (!event.isSneaking() && dropPassengerTask != null) {
                dropPassengerTask.cancel();
                dropPassengerTask = null;
            }
        }

        @Override
        public void onBeingTargeted(LivingEntity carrier, LivingEntity entity, EntityTargetLivingEntityEvent event) {
            if (carrier.getPassengers().contains(entity))
                event.setCancelled(true);
        }

        @Override
        public void onCarrierTarget(LivingEntity carrier, LivingEntity target, EntityTargetLivingEntityEvent event) {
        }
    }
}
