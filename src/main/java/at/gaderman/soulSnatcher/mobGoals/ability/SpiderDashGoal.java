package at.gaderman.soulSnatcher.mobGoals.ability;

import at.gaderman.soulSnatcher.SoulSnatcher;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class SpiderDashGoal extends SoulAbilityGoal{
    public SpiderDashGoal(Mob mob) {
        super(mob);
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_spider_dash"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    private long lastJump;
    private static final long JUMP_COOLDOWN = 5000;

    @Override
    public boolean shouldActivate() {
        return mob.getTarget() != null && lastJump < System.currentTimeMillis() - JUMP_COOLDOWN;
    }

    @Override
    public void start() {
        lastJump = System.currentTimeMillis();
        mob.setVelocity(mob.getVelocity().add(mob.getLocation().getDirection().multiply(0.95).setY(0.4)));

        mob.getWorld().spawnParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 0.2, 0), 1, 0, 0, 0, 0);
        mob.getWorld().playSound(mob, Sound.ENTITY_SPIDER_AMBIENT, 1f, 1.3f);
    }
}
