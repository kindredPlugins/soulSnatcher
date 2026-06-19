package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.ability.PhantomAttackGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnSneakToggleTrigger;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class PhantomSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new PhantomSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "phantom_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.PHANTOM;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "7e95153ec23284b283f00d19d29756f244313a061b70ac03b97d236ee57bd982";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Phantom Soul", TextColor.color(0x5061a4));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }

    public static class PhantomSoulInstance extends SoulInstance implements OnSneakToggleTrigger {
        protected PhantomSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);

            if (carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new PhantomAttackGoal(mob, this));
        }

        private boolean gliding;

        public void activateGliding(){
            gliding = true;
            LivingEntity carrier = carrier();

            Vector velocity = carrier.getVelocity();
            if(velocity.getY() < 0) carrier.setVelocity(velocity.setY(velocity.getY() * 0.95));

            final double initialDownwardSpeed = Math.max(0.0, -carrier.getVelocity().getY());
            if (initialDownwardSpeed > 0) {
                Vector vel = carrier.getVelocity();
                vel.setY(-initialDownwardSpeed * 0.6);
                carrier.setVelocity(vel);
            }

            final long startTick = Bukkit.getCurrentTick();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!gliding || !carrier.isValid() || carrier.isOnGround()) {
                        if (carrier.isValid() && carrier instanceof Player player) player.stopSound(Sound.ITEM_ELYTRA_FLYING);
                        cancel();
                        return;
                    }

                    if (carrier.getFallDistance() >= 4) carrier.setFallDistance(carrier.getFallDistance() * 0.8f);

                    long ticks = Bukkit.getCurrentTick() - startTick;

                    // decay curve: starts stronger and asymptotically approaches 0.1
                    // factor(t) = 0.1 + 0.9 * exp(-k * t)
                    double k = 0.06;
                    double decayFactor = 0.1 + 0.9 * Math.exp(-k * ticks);

                    double desiredDownward = Math.max(initialDownwardSpeed * decayFactor, 0.1);
                    double desiredY = -desiredDownward;

                    Vector currentVelocity = carrier.getVelocity();
                    double smooth = 0.18;
                    double newY = currentVelocity.getY() * (1 - smooth) + desiredY * smooth;

                    if (newY > 0) newY = Math.min(newY, 0);

                    currentVelocity.setY(newY);
                    currentVelocity.add(carrier.getLocation().getDirection().normalize().setY(0).multiply(0.05));
                    carrier.setVelocity(currentVelocity);

                    if (ticks % 5 == 0) {
                        carrier.getWorld().spawnParticle(Particle.END_ROD, carrier.getLocation(), 5, 0.1, 0, 0.1, 0.01);
                        carrier.getWorld().playSound(carrier, Sound.ITEM_ELYTRA_FLYING, 0.25f, 0.75f);
                    }

                }
            }.runTaskTimer(SoulSnatcher.getPlugin(), 1L, 1L);
        }

        @Override
        public void onSneakToggle(Player carrier, PlayerToggleSneakEvent event) {
            if(!gliding){
                activateGliding();
            }

            gliding = event.isSneaking();
        }

        @Override
        protected void cleanUp() {
            gliding = false;
        }
    }
}
