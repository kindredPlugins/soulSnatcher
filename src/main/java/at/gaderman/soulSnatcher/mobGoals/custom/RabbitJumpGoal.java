package at.gaderman.soulSnatcher.mobGoals.custom;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.SoulOwnerGoal;
import at.gaderman.soulSnatcher.souls.instances.movement.RabbitSoulType;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class RabbitJumpGoal extends SoulOwnerGoal {
    private final RabbitSoulType.RabbitSoulInstance soulInstance;

    public RabbitJumpGoal(Mob mob, RabbitSoulType.RabbitSoulInstance soulInstance) {
        super(mob);
        this.soulInstance = soulInstance;
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_rabbit_jumping"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    @Override
    public boolean shouldActivate() {
        if (!mob.getPathfinder().hasPath())
            return false;

        if (mob.getTarget() == null && mob.getPathfinder().getCurrentPath() != null && mob.getPathfinder().getCurrentPath().getFinalPoint() != null)
            return mob.getLocation().distanceSquared(mob.getPathfinder().getCurrentPath().getFinalPoint()) > 25;

        return mob.getTarget() != null && mob.getTarget().getWorld().equals(mob.getWorld()) && mob.getTarget().getLocation().distanceSquared(mob.getLocation()) > 15;
    }

    @Override
    public boolean shouldStayActive() {
        return shouldActivate();
    }

    @Override
    public void tick() {
        super.tick();

        if (mob.isOnGround()) {
            if(mob.getTarget() != null)
                mob.lookAt(mob.getTarget());

            soulInstance.boost(mob);
            mob.setVelocity(mob.getVelocity().multiply(2f));
        }
    }
}
