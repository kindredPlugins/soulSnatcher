package at.gaderman.soulSnatcher.mobGoals.targeting;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.SoulAbilityGoal;
import at.gaderman.soulSnatcher.souls.instances.movement.DolphinSoulType;
import at.gaderman.soulSnatcher.utils.BlockUtils;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

public class WaterMovementGoal extends SoulAbilityGoal {

    public WaterMovementGoal(Mob mob) {
        super(mob);
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "water_movement"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    private long lastScan;
    private static final int SCAN_COOLDOWN = 250;

    private Block waterTarget;

    @Override
    public boolean shouldActivate() {
        LivingEntity target = mob.getTarget();
        if (target == null) return false;

        if(mob.isUnderWater()){
            if(Bukkit.getCurrentTick() % 5 == 0 && mob.getNoDamageTicks() == 0) {
                Vector targetVector = target.getLocation().toVector().subtract(mob.getLocation().toVector());
                mob.setVelocity(mob.getLocation().getDirection().setY(targetVector.getY()).normalize().multiply(0.25));
                //mob.setVelocity(targetVector.normalize().multiply(0.3));
                mob.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, mob.getLocation().clone().add(0, 1, 0), 20, 1, 0.5, 1, 0.25);
            }

            return false;
        }

        if (lastScan > System.currentTimeMillis() - SCAN_COOLDOWN || (!mob.isOnGround() && !mob.isInWater())) return false;

        lastScan = System.currentTimeMillis();
        double distance = mob.getLocation().distance(target.getLocation());
        if (distance <= 5) return false;

        Optional<Block> possWater = BlockUtils.checkForBlocks(mob.getLocation().clone().add(-7, -1, -7), mob.getLocation().clone().add(7, 1, 7), block -> {
                    if (block.getType() != Material.WATER) return false;
                    return mob.hasLineOfSight(block.getLocation());
                })
                .stream()
                .min(Comparator.comparing(block -> mob.getLocation().distanceSquared(block.getLocation())));

        if (possWater.isEmpty()) return false;

        waterTarget = possWater.get();
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        return waterTarget != null && waterTarget.getType() == Material.WATER && mob.hasLineOfSight(waterTarget.getLocation());
    }

    @Override
    public void tick() {
        mob.getPathfinder().moveTo(waterTarget.getLocation(), 1.5);

        if(mob.isInWater() && mob.getTarget() != null){
            DolphinSoulType.DolphinSoulInstance.doDolphinJump(mob,
                    mob.getTarget().getLocation().toVector().subtract(mob.getLocation().toVector()));
            waterTarget = null;
        }
    }
}
