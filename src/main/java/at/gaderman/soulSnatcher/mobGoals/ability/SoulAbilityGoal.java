package at.gaderman.soulSnatcher.mobGoals.ability;

import at.gaderman.soulSnatcher.mobGoals.SoulOwnerGoal;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public abstract class SoulAbilityGoal extends SoulOwnerGoal {
    public SoulAbilityGoal(Mob mob) {
        super(mob);
    }

    private LivingEntity target;
    private long lastActivation;

    @Override
    public boolean shouldActivate() {
        return target != null && mob.getTarget() != null && lastActivation < System.currentTimeMillis() - activationCooldown();
    }

    @Override
    public void start() {
        super.start();
        lastActivation = System.currentTimeMillis();
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    protected abstract int activationCooldown();

    public final void setTarget(@Nullable LivingEntity target) {
        this.target = target;
    }
}
