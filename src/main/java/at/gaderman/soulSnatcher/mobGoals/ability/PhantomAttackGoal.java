package at.gaderman.soulSnatcher.mobGoals.ability;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.instances.movement.PhantomSoulType;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class PhantomAttackGoal extends SoulAbilityGoal{
    private final PhantomSoulType.PhantomSoulInstance phantomSoul;

    public PhantomAttackGoal(Mob mob, PhantomSoulType.PhantomSoulInstance phantomSoul) {
        super(mob);

        this.phantomSoul = phantomSoul;
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_phantom_attack"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    private long lastFlight;
    private static final long FLYOFF_COOLDOWN = 5000;

    @Override
    public boolean shouldActivate() {
        return mob.getTarget() != null && lastFlight < System.currentTimeMillis() - FLYOFF_COOLDOWN && mob.isOnGround();
    }

    @Override
    public void start() {
        lastFlight = System.currentTimeMillis();
        mob.setVelocity(new Vector(0, 1.5, 0));

        mob.getWorld().spawnParticle(Particle.CLOUD, mob.getLocation().add(0, 0.2, 0), 100, 0.1, 0, 0.1, 0.1);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_CAT_HISS, 2f, 1.5f);

        Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), phantomSoul::activateGliding, 20L);
    }
}
