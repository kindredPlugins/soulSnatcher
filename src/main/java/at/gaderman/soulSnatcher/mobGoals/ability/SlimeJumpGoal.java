package at.gaderman.soulSnatcher.mobGoals.ability;

import at.gaderman.soulSnatcher.SoulSnatcher;
import com.destroystokyo.paper.entity.ai.GoalKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class SlimeJumpGoal extends SoulAbilityGoal {
    public SlimeJumpGoal(Mob mob) {
        super(mob);
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_slime_jump"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    protected int activationCooldown() {
        return 5000;
    }

    @Override
    public void start() {
        super.start();
        mob.setVelocity(mob.getVelocity().add(new Vector(0, 1, 0)));

        Particle.ITEM_SLIME.builder()
                .location(mob.getLocation().clone().add(0, 0.5, 0))
                .offset(0.5, 0.3, 0.5)
                .count(40)
                .receivers(16)
                .spawn();
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_SLIME_JUMP, 1f, 1.3f);
    }
}
