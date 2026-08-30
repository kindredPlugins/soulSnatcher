package at.gaderman.soulSnatcher.souls.instances.utility.event;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.action.OnEntityEquipmentTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@AutoService(SoulType.class)
public class AllaySoulType extends SoulType {
    @Override
    public @NotNull SoulInstance<AllaySoulType> create(LivingEntity carrier) {
        return new AllaySoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "allay_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.ALLAY;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "df5de940bfe499c59ee8dac9f9c3919e7535eff3a9acb16f4842bf290f4c679f";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.UTILITY;
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Allay Soul", TextColor.color(0x64fbff));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When holding an item, all nearby items "),
                Component.text("of the same sort will be ")
                        .append(Component.text("pulled ", NamedTextColor.GREEN))
                        .append(Component.text("towards you.", NamedTextColor.WHITE))
        );
    }

    @Override
    public boolean isInvalidInfusionTarget(LivingEntity entity) {
        return super.isInvalidInfusionTarget(entity) || (!(entity instanceof Zombie) && !(entity instanceof Skeleton)
                && !(entity instanceof Piglin));
    }

    public static class AllaySoulInstance extends SoulInstance<AllaySoulType> implements OnEntityEquipmentTrigger {

        protected AllaySoulInstance(LivingEntity carrier, AllaySoulType soulType) {
            super(carrier, soulType);

            final boolean isCarrierPlayer = carrier instanceof Player;

            if (isCarrierPlayer)
                updateActiveMagnets();

            if (carrier instanceof Mob mob)
                mob.setCanPickupItems(true);

            magnetTask = carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), _ -> {
                if (isCarrierPlayer && activeMagnets.isEmpty())
                    return;

                carrier.getWorld().getNearbyEntitiesByType(Item.class, carrier.getLocation(), 5, 5, 5)
                        .forEach(item -> {
                            if (item.getThrower() != null && item.getThrower().equals(carrier.getUniqueId()))
                                return;

                            if (isCarrierPlayer && activeMagnets.stream().noneMatch(magnet -> magnet.isSimilar(item.getItemStack())))
                                return;

                            if (!carrier.hasLineOfSight(item))
                                return;

                            Vector direction = carrier.getLocation().subtract(item.getLocation()).toVector().normalize();
                            item.setVelocity(direction.multiply(1.3));

                            item.getWorld().playSound(item.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 0.5f, 0.5f);
                            item.getWorld().spawnParticle(Particle.DUST, item.getLocation(), 1,
                                    0, 0, 0, 0, new Particle.DustOptions(Color.AQUA, 1));
                        });

            }, () -> magnetTask = null, 10L, 10L);
        }

        private ScheduledTask magnetTask;
        private final List<ItemStack> activeMagnets = new ArrayList<>();

        @Override
        public void onEntityEquipmentChange(LivingEntity carrier, EntityEquipmentChangedEvent event) {
            if (!(carrier instanceof Player))
                return;

            updateActiveMagnets();
        }

        private void updateActiveMagnets() {
            activeMagnets.clear();

            EntityEquipment equipment = carrier().getEquipment();
            if (equipment == null)
                return;

            if (!equipment.getItemInMainHand().isEmpty()) activeMagnets.add(equipment.getItemInMainHand());
            if (!equipment.getItemInOffHand().isEmpty()) activeMagnets.add(equipment.getItemInOffHand());
        }

        @Override
        protected void cleanUp() {
            if (magnetTask != null)
                magnetTask.cancel();
        }
    }
}
