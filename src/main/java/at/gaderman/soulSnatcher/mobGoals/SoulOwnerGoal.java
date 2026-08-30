package at.gaderman.soulSnatcher.mobGoals;

import com.destroystokyo.paper.entity.ai.Goal;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a mob goal which introduced a new ability, this essentially just means that the goal needs a reference
 * to the mob it belongs to
 */
public abstract class SoulOwnerGoal implements Goal<@NotNull Mob> {

    protected final Mob mob;

    public SoulOwnerGoal(Mob mob){
        this.mob = mob;
    }

}
