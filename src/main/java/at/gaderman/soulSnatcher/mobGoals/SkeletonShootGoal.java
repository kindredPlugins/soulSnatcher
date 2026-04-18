package at.gaderman.soulSnatcher.mobGoals;

import at.gaderman.soulSnatcher.SoulSnatcher;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

//TODO: make a uniform goal, more goals will come in the future probably, there is base line potential definitely, also config will play some role
/**
 * A custom behaviour mob goal that adds an ability to mobs. While pursuing a target wil spawn in a bow
 * who will after some loading time make the mob shoot an arrow at facing direction. This ability has a small
 * cooldown which will be periodically activated as long as the mob has a valid target.
 */
public class SkeletonShootGoal implements Goal<@NotNull Mob> {

    private final Mob mob;

    public SkeletonShootGoal(Mob mob){
        this.mob = mob;
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_skeleton_shoot"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    private long lastShot;
    private static final long SHOT_COOLDOWN = 5000;

    @Override
    public boolean shouldActivate() {
        return mob.getTarget() != null && lastShot < System.currentTimeMillis() - SHOT_COOLDOWN;
    }

    private long drawingTicks;
    private static final long DRAW_TILL_SHOOT = 20L;

    @Override
    public boolean shouldStayActive() {
        return drawingTicks <= DRAW_TILL_SHOOT;
    }

    private ItemDisplay bow;

    @Override
    public void start() {
        Location bowSource = mob.getLocation().add(0, mob.getEyeHeight(true) + 0.5, 0);
        bowSource.getWorld().spawnParticle(Particle.WHITE_SMOKE, bowSource, 5);
        bowSource.getWorld().playSound(bowSource, Sound.ENTITY_ARROW_SHOOT, 3f, 0.25f);

        bow = mob.getWorld().spawn(bowSource, ItemDisplay.class, display -> {
            display.setItemStack(ItemStack.of(Material.BOW));
            display.setBillboard(Display.Billboard.FIXED);
        });
        mob.addPassenger(bow);

        SoulSnatcher.getPlugin().registerDelayedTask(bow::remove, 2 * DRAW_TILL_SHOOT + 1);
    }

    @Override
    public void tick() {
        drawingTicks++;

        bow.getWorld().spawnParticle(Particle.CRIT, bow.getLocation(), 1);
    }

    @Override
    public void stop() {
        drawingTicks = 0;
        lastShot = System.currentTimeMillis();

        mob.getWorld().playSound(mob, Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
        mob.launchProjectile(Arrow.class, mob.getLocation().getDirection().multiply(2));
    }

}
