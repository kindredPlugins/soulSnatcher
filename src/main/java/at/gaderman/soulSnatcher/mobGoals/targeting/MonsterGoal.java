package at.gaderman.soulSnatcher.mobGoals.targeting;

import at.gaderman.soulSnatcher.SoulSnatcher;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * A mob goal that is added to passive mobs in order for them to support aggression and attacking players
 */
public class MonsterGoal implements Goal<@NotNull Mob> {

    private final Mob mob;

    public MonsterGoal(Mob mob){
        super();
        this.mob = mob;

        boolean hasNoAttack = mob.getAttribute(Attribute.ATTACK_DAMAGE) == null;


        if(hasNoAttack) {
            mob.registerAttribute(Attribute.ATTACK_DAMAGE);
            mob.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(mob.getWorld().getDifficulty().getValue() - 1);
        }
    }

    private static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "monster_goal"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }

    private long lastScan;
    private static final long SCAN_INTERVAL = 500;

    @Override
    public boolean shouldActivate() {
        return mob.getTarget() == null && lastScan < System.currentTimeMillis() - SCAN_INTERVAL;
    }

    @Override
    public boolean shouldStayActive() {
        return mob.getTarget() != null;
    }

    @Override
    public void start() {
        lastScan = System.currentTimeMillis();

        var optPlayer = mob.getWorld().getNearbyPlayers(mob.getLocation(), 16)
                .stream()
                .filter(player -> !player.isInvisible() && !player.isInvulnerable() && !player.getGameMode().isInvulnerable())
                .min(Comparator.comparing(player -> player.getLocation().distanceSquared(mob.getLocation())));

        if(optPlayer.isEmpty()) return;
        mob.setTarget(optPlayer.get());
    }

    private long lastAttack;
    private static final long ATTACK_COOLDOWN = 200;

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if(target == null || lastAttack > System.currentTimeMillis() - ATTACK_COOLDOWN) return;

        mob.getPathfinder().moveTo(target, 1.25);

        if(mob.getLocation().distanceSquared(target.getLocation()) <= 2.25) {
            mob.attack(target);
            lastAttack = System.currentTimeMillis();
        }
    }

}
