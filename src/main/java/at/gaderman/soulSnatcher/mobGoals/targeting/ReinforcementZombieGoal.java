package at.gaderman.soulSnatcher.mobGoals.targeting;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.SoulAbilityGoal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ReinforcementZombieGoal extends SoulAbilityGoal {
    private final LivingEntity owner;

    public ReinforcementZombieGoal(Mob mob, LivingEntity owner) {
        super(mob);
        this.owner = owner;
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "reinforcement_zombie"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    private static final double FOLLOW_RANGE = 50 ^ 2;

    @Override
    public boolean shouldActivate() {
        return owner.isValid() && mob.getTarget() == null && mob.getWorld().equals(owner.getWorld()) && mob.getLocation().distanceSquared(owner.getLocation()) >= FOLLOW_RANGE;
    }

    @Override
    public boolean shouldStayActive() {
        return false;
    }

    @Override
    public void start() {
        Location to = owner.getLocation().add(mob.getLocation().subtract(owner.getLocation()).toVector().normalize().multiply(1.5));
        mob.getPathfinder().moveTo(to);
    }
}
