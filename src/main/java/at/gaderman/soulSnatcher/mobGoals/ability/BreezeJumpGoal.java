package at.gaderman.soulSnatcher.mobGoals.ability;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.instances.movement.BreezeSoulType;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class BreezeJumpGoal extends SoulAbilityGoal{

    private final BreezeSoulType.BreezeSoulInstance soulInstance;

    public BreezeJumpGoal(Mob mob, BreezeSoulType.BreezeSoulInstance soulInstance) {
        super(mob);
        this.soulInstance = soulInstance;
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_breeze_jump"));

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
        return mob.getTarget() != null && soulInstance.canJump();
    }

    @Override
    public void start() {
        soulInstance.breezeJump(mob);
        mob.setVelocity(mob.getVelocity().add(mob.getLocation().getDirection().multiply(0.5f).setY(1)));
    }
}
