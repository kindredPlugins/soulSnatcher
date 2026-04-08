package at.gaderman.soulSnatcher.souls.instances.monsters;

import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.triggers.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.BlockUtils;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class ZombieSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new ZombieSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "zombie_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.ZOMBIE;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "783aaaee22868cafdaa1f6f4a0e56b0fdb64cd0aeaabd6e83818c312ebe66437";
    }

    @Override
    protected @NotNull Component displayName() {
        return Component.text("Zombie Soul", NamedTextColor.DARK_GREEN);
    }

    @Override
    protected @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When being hit summons a ")
                        .append(Component.text("reinforcement zombie", NamedTextColor.AQUA)),
                Component.text("nearby who will aid you in combat")
        );
    }

    static class ZombieSoulInstance extends SoulInstance implements OnDamageReceivedTrigger {
        protected ZombieSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {

        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            Location spawnLoc = BlockUtils.findSpreadLocation(carrier.getLocation(), 8, 16);
            Zombie zombie = carrier.getWorld().spawn(spawnLoc == null ? carrier.getLocation() : spawnLoc, Zombie.class);
            zombie.addScoreboardTag(SoulType.NO_SOUL_RELEASE_TAG);
            zombie.setTarget(damager);

            if (carrier instanceof Player player) {
                var equipment = zombie.getEquipment();
                equipment.setHelmet(ItemUtils.getHeadOfPlayer(player));
                equipment.setHelmetDropChance(0.0f);
            }
        }
    }
}
