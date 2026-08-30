package at.gaderman.soulSnatcher.souls.instances.movement;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.mobGoals.custom.BreezeJumpGoal;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.souls.triggers.input.OnPlayerJumpTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@AutoService(SoulType.class)
public class BreezeSoulType extends ConfigHoldingSoulType {
    @Override
    public @NotNull SoulInstance<BreezeSoulType> create(LivingEntity carrier) {
        return new BreezeSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "breeze_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.BREEZE;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "a275728af7e6a29c88125b675a39d88ae9919bb61fdc200337fed6ab0c49d65c";
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.MOVEMENT;
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Breeze Soul", TextColor.color(0x6c76ab));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("Sprint", NamedTextColor.GOLD)
                        .append(Component.text(" + Jump "))
                        .append(Component.text("while looking down to", NamedTextColor.WHITE)),
                Component.text("shoot a wind charge under your feet."),
                Component.text( jumpCooldown.cached() / 20.0 + "s ", NamedTextColor.AQUA)
                        .append(Component.text("after hitting ground resets.", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String JUMP_COOLDOWN_CONFIG_ID = "jump_cooldown";

    private final ConfigOption<Integer> jumpCooldown = configOption(JUMP_COOLDOWN_CONFIG_ID, 50, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                JUMP_COOLDOWN_CONFIG_ID, "Cooldown for being able to do the wind charge jump after ground has been touched in ticks (20 ticks = 1 second)"
        );
    }

    //endregion

    public static class BreezeSoulInstance extends SoulInstance<BreezeSoulType> implements OnPlayerJumpTrigger, OnDamageReceivedTrigger {
        protected BreezeSoulInstance(LivingEntity carrier, BreezeSoulType soulType) {
            super(carrier, soulType);

            if (carrier instanceof Mob mob)
                Bukkit.getMobGoals().addGoal(mob, 0, new BreezeJumpGoal(mob, this));
        }

        private boolean canJump = true;
        // replaced with config option

        @Override
        public void onPlayerJump(Player carrier, PlayerJumpEvent event) {
            if (!carrier.isSprinting() || !canJump) return;
            if (carrier.getPitch() < 70) return;

            breezeJump(carrier);
        }

        public void breezeJump(LivingEntity carrier) {
            canJump = false;
            carrier.getWorld().spawn(carrier.getLocation(), WindCharge.class, charge -> {
                charge.setShooter(carrier);
                charge.setVelocity(new Vector(0, -1, 0));
            });
            carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1f, 1f);

            carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), task -> {
                RayTraceResult result = carrier.getWorld().rayTraceBlocks(carrier.getLocation(), new Vector(0, -1, 0), 0.2, FluidCollisionMode.ALWAYS, true);
                if (result == null) return;

                task.cancel();
                carrier.getScheduler().runDelayed(SoulSnatcher.getPlugin(), _ -> {
                    canJump = true;

                    if (carrier instanceof Player player)
                        player.playSound(player, Sound.ENTITY_BREEZE_CHARGE, 1f, 1f);
                }, null, soulType().jumpCooldown.cached());
            }, null, 1L, 1L);
        }

        public boolean canJump() {
            return canJump;
        }

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if(carrier instanceof Mob && event.getCause() == EntityDamageEvent.DamageCause.FALL)
                event.setCancelled(true);
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {

        }
    }
}
