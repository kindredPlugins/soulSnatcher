package at.gaderman.soulSnatcher.mobGoals.ability;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.SoulOwnerGoal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class GuardianAttackGoal extends SoulAbilityGoal {
    public GuardianAttackGoal(Mob mob) {
        super(mob);
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_guardian_attack"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    private int laserTicks;
    private boolean finishedAttacking;

    private static final int LASER_CHARGE_TICKS = 50;

    @Override
    public boolean shouldActivate() {
        return super.shouldActivate() && hasValidTarget();
    }

    @Override
    public boolean shouldStayActive() {
        return !finishedAttacking && hasValidTarget();
    }

    private boolean hasValidTarget(){
        LivingEntity target = mob.getTarget();
        return target != null && mob.hasLineOfSight(target)
                && mob.getLocation().distanceSquared(target.getLocation()) <= 15 * 15;
    }

    @Override
    protected int activationCooldown() {
        return 3000 + LASER_CHARGE_TICKS;
    }

    @Override
    public void start() {
        super.start();
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GUARDIAN_AMBIENT, 1f, 1f);
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        assert target != null;

        if (laserTicks >= LASER_CHARGE_TICKS) {
            double damage = (target instanceof Player player && player.isBlocking()) ? 3 : 6;

            target.damage(damage, DamageSource.builder(DamageType.MAGIC)
                    .withDirectEntity(mob)
                    .withCausingEntity(mob)
                    .build()
            );
            target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getEyeLocation(), 70, 0.5, 0.5, 0.5, 0.3);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 2f);
            finishedAttacking = true;
            return;
        }

        Vector vector = target.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize();

        Location origin = mob.getEyeLocation();
        double distance = target.getLocation().distance(mob.getEyeLocation());

        if(laserTicks % 3 == 0) {
            for (double i = 0; i < distance; i += 0.5) {
                Location location = origin.clone().add(vector.clone().multiply(i));
                location.getWorld().spawnParticle(Particle.ENCHANTED_HIT, location, 1, 0, 0, 0, 0);
            }

            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, 1f, 1f);
        }

        laserTicks++;
    }

    @Override
    public void stop() {
        laserTicks = 0;
        finishedAttacking = false;
    }
}
