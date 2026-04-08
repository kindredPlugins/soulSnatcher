package at.gaderman.soulSnatcher.souls;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * The abstract definition of a Soul.
 * A soul is an object that can be bound to an entity introducing new abilities or mechanic changes
 */
public abstract class SoulInstance {

    private final LivingEntity carrier;
    private final SoulType soulType;

    protected SoulInstance(LivingEntity carrier, SoulType soulType){
        this.carrier = carrier;
        this.soulType = soulType;
    }

    public LivingEntity carrier() { return carrier; }
    public SoulType soulType() { return soulType; }

    protected final boolean isPlayerBound() {
        return carrier instanceof Player;
    }

    protected final boolean isInfused() {
        return !(carrier instanceof Player);
    }


}
