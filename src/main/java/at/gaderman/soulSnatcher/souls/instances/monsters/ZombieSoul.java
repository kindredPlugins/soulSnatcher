package at.gaderman.soulSnatcher.souls.instances.monsters;

import at.gaderman.soulSnatcher.souls.Soul;
import at.gaderman.soulSnatcher.souls.triggers.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.BlockUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

@AutoService(Soul.class)
public class ZombieSoul extends Soul implements OnDamageReceivedTrigger {
    @Override
    public String id() {
        return "zombie_soul";
    }

    @Override
    public EntityType entityType() {
        return EntityType.ZOMBIE;
    }

    @Override
    protected String skullTexture() {
        return "783aaaee22868cafdaa1f6f4a0e56b0fdb64cd0aeaabd6e83818c312ebe66437";
    }

    @Override
    protected Component displayName() {
        return Component.text("Zombie Soul", NamedTextColor.DARK_GREEN);
    }

    @Override
    protected List<Component> description() {
        return List.of();
    }

    @Override
    public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {

    }

    @Override
    public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
        Location spawnLoc = BlockUtils.findSpreadLocation(carrier.getLocation(), 16, 16);
        Zombie zombie = carrier.getWorld().spawn(spawnLoc == null ? carrier.getLocation() : spawnLoc, Zombie.class);
        zombie.addScoreboardTag(Soul.NO_SOUL_RELEASE_TAG);
    }
}
