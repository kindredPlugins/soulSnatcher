package at.gaderman.soulSnatcher.mobGoals.targeting;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.SoulAbilityGoal;
import at.gaderman.soulSnatcher.souls.instances.utility.event.PigSoulType;
import com.destroystokyo.paper.entity.RangedEntity;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

public class SearchAndAddPassengerGoal extends SoulAbilityGoal {

    public SearchAndAddPassengerGoal(Mob mob) {
        super(mob);
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "search_rider"));

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

    private Monster riderTarget;

    @Override
    public boolean shouldActivate() {
        if (!mob.getPassengers().isEmpty() || mob.isInsideVehicle()) return false;
        if (lastScan > System.currentTimeMillis() - SCAN_COOLDOWN) return false;

        lastScan = System.currentTimeMillis();

        Optional<Monster> possibleRider = mob.getWorld().getNearbyEntitiesByType(Monster.class, mob.getLocation(), 24)
                .stream()
                .filter(monster -> !monster.equals(mob) && PigSoulType.PigSoulInstance.isValidPassengerTarget(monster)
                        && monster.getPassengers().isEmpty() && !monster.isInsideVehicle() && mob.hasLineOfSight(monster))
                .min(Comparator.comparing(monster -> {
                    double distance = monster.getLocation().distance(mob.getLocation());

                    return monster instanceof RangedEntity ? distance / 4 : distance;
                }));
        if (possibleRider.isEmpty()) return false;

        riderTarget = possibleRider.get();
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        return riderTarget != null && riderTarget.isValid() && !riderTarget.isInsideVehicle();
    }

    @Override
    public void tick() {
        mob.getPathfinder().moveTo(riderTarget, 1.3);

        double distanceSquared = mob.getLocation().distanceSquared(riderTarget.getLocation());
        if(distanceSquared > Math.pow(mob.getWidth(), 2) * 1.5) return;

        mob.addPassenger(riderTarget);
        mob.getWorld().playSound(mob, Sound.ENTITY_PIG_SADDLE, 1f, 0.5f);

        riderTarget = null;
    }
}
