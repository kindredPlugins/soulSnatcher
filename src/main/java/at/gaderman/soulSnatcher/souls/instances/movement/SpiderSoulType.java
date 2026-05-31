package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.mobGoals.SpiderDashGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.input.OnPlayerJumpTrigger;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(SoulType.class)
public class SpiderSoulType extends SoulType {
    @Override
    public @NotNull SoulInstance create(LivingEntity carrier) {
        return new SpiderSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "spider_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.SPIDER;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "35e248da2e108f09813a6b848a0fcef111300978180eda41d3d1a7a8e4dba3c3";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Spider Soul", TextColor.color(0x9c0000));
    }

    @Override
    public @NotNull List<Component> description() {
        return List.of();
    }

    public static class SpiderSoulInstance extends SoulInstance implements OnPlayerJumpTrigger {
        protected SpiderSoulInstance(LivingEntity carrier, SoulType soulType) {
            super(carrier, soulType);

            if(carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new SpiderDashGoal(mob));
        }

        //TODO: movement mechanics are op in minecraft might look into this later but requires some testing from actual players and gameplay to see

        private long lastJump;
        private static final long JUMP_COOLDOWN = 1000;

        @Override
        public void onPlayerJump(Player carrier, PlayerJumpEvent event) {
            if(!carrier.isSneaking() || lastJump > System.currentTimeMillis() - JUMP_COOLDOWN) return;

            lastJump = System.currentTimeMillis();

            carrier.setVelocity(carrier.getVelocity().add(carrier.getLocation().getDirection().multiply(0.75).setY(0.2)));
            carrier.setExhaustion(carrier.getExhaustion() + 2.75f);

            carrier.getWorld().spawnParticle(Particle.SWEEP_ATTACK, carrier.getLocation().add(0, 0.2, 0), 1, 0, 0, 0, 0);
            carrier.getWorld().playSound(carrier, Sound.ENTITY_SPIDER_AMBIENT, 1f, 1.5f);

        }
    }
}
