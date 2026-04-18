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

    /**
     * If this soul is player-bound. This essentially means that the carrier is a player.
     * This is important since sometimes effects differ between player-bound and infusion.
     * @return If this soul belongs to a player and is by thus player-bound
     */
    protected final boolean isPlayerBound() {
        return carrier instanceof Player;
    }

    /**
     * If this soul is an infusion. This essentially means that the carrier is a mob.
     * This is important since sometimes effects differ between player-bound and infusion.
     * @return If this soul belongs to a mob and is by thus an infusion
     */
    protected final boolean isInfused() {
        return !(carrier instanceof Player);
    }

    /**
     * Called when this soulInstance is removed. Is important for some souls in order to clean up abilities.
     */
    protected void cleanUp(){}
}
