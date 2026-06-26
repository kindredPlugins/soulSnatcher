package at.gaderman.soulSnatcher.souls.instances.combat.targeting;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoService(SoulType.class)
public class BlazeSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<BlazeSoulType> create(LivingEntity carrier) {
        return new BlazeSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "blaze_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.BLAZE;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "b20657e24b56e1b2f8fc219da1de788c0c24f36388b1a409d0cd2d8dba44aa3b";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Blaze Soul", TextColor.color(0xfc9600));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text("When engaging in combat activate ")
                        .append(Component.text("Aura", NamedTextColor.GOLD))
                        .append(Component.text(".", NamedTextColor.WHITE)),
                Component.text("Enemies who enter your close proximity"),
                Component.text("will be struck down.")
        );
    }

    //region Config Values

    private static final String AURA_TIMEOUT_CONFIG_ID = "aura_timeout";
    private static final String AURA_RANGE_CONFIG_ID = "aura_range";
    private static final String AURA_DAMAGE_CONFIG_ID = "aura_damage";
    private static final String AURA_HIT_COOLDOWN_CONFIG_ID = "aura_hit_cooldown";
    private static final String AURA_WINDUP_CONFIG_ID = "aura_windup";

    private final ConfigOption<Integer> auraTimeout = configOption(AURA_TIMEOUT_CONFIG_ID, 30000, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Double> auraRange = configOption(AURA_RANGE_CONFIG_ID, 2.4, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Double> auraDamage = configOption(AURA_DAMAGE_CONFIG_ID, 6.0, FileConfiguration::getDouble, value -> Math.max(value, 0));
    private final ConfigOption<Integer> auraHitCooldown = configOption(AURA_HIT_COOLDOWN_CONFIG_ID, 2000, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Integer> auraWindUp = configOption(AURA_WINDUP_CONFIG_ID, 4, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                AURA_TIMEOUT_CONFIG_ID, "After how much time without casting a hit should aura disable itself in milliseconds (1000ms = 1s)",
                AURA_RANGE_CONFIG_ID, "In which sphere around the player aura can hit entities (XYZ hitbox detection)",
                AURA_DAMAGE_CONFIG_ID, "How much damage a hit from aura does",
                AURA_HIT_COOLDOWN_CONFIG_ID, "How much cooldown time there is between each aura hit in milliseconds (1000ms = 1s)",
                AURA_WINDUP_CONFIG_ID, "Time between aura dealing damage and a target stepping in ticks (20 ticks = 1 second)"
        );
    }

    //endregion

    public static class BlazeSoulInstance extends TargetTrackerSoulInstance<BlazeSoulType> {
        protected BlazeSoulInstance(LivingEntity carrier, BlazeSoulType soulType) {
            super(carrier, soulType);

            if (!combatTargets.isEmpty())
                auraTask = createAuraTask();
        }

        private long lastAuraHit;
        private BukkitTask auraTask;

        private BukkitTask createAuraTask() {
            lastAuraHit = System.currentTimeMillis();

            return new BukkitRunnable() {
                final LivingEntity carrier = carrier();

                @Override
                public void run() {
                    boolean isActive = lastAuraHit < System.currentTimeMillis() - soulType().auraHitCooldown.cached();

                    if (isActive) {
                        carrier.getWorld().spawnParticle(Particle.FLAME, carrier.getLocation().add(0, 1, 0),
                                1, 0.1, 0.5, 0.1, 0.01);
                    }

                    if (combatTargets.isEmpty()) {
                        extinguish();
                        return;
                    }

                    if (Bukkit.getCurrentTick() % 10 == 0)
                        combatTargets = combatTargets.stream().filter(LivingEntity::isValid).collect(Collectors.toSet());

                    if (isActive) {
                        for (LivingEntity target : carrier.getWorld().getNearbyLivingEntities(carrier.getLocation(), soulType().auraRange.cached())) {
                            if (target.getNoDamageTicks() != 0 || !combatTargets.contains(target)) continue;

                            if (carrier.getWorld().rayTraceBlocks(carrier.getEyeLocation(), target.getLocation().toVector().subtract(carrier.getLocation().toVector()),
                                    (Math.sqrt(2 * soulType().auraRange.cached()))) != null)
                                return;

                            lastAuraHit = System.currentTimeMillis();

                            Bukkit.getScheduler().runTaskLater(SoulSnatcher.getPlugin(), () -> {
                                if (!target.isValid() || !carrier.isValid())
                                    return;

                                if(!carrier.getWorld().getNearbyLivingEntities(carrier.getLocation(), soulType().auraRange.cached()).contains(target))
                                    return;

                                target.damage(soulType().auraDamage.cached(), DamageSource.builder(DamageType.MOB_ATTACK)
                                        .withDirectEntity(carrier)
                                        .withCausingEntity(carrier)
                                        .build());

                                Location hitLoc = target.getEyeLocation().add(0, -0.2, 0);
                                target.getWorld().spawnParticle(Particle.LAVA, hitLoc, 30);
                                target.getWorld().spawnParticle(Particle.FLAME, hitLoc, 30, 0.3, 0.3, 0.3, 0.01);
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 1f, 1.5f);
                            }, soulType().auraWindUp.cached());

                            target.getWorld().spawnParticle(Particle.FLAME, target.getEyeLocation().add(0, -0.2, 0), 5, 0.3, 0.3, 0.3, 0);
                            target.getWorld().playSound(target.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 0.5f, 2f);
                            break;
                        }
                    }

                    if (lastAuraHit < System.currentTimeMillis() - soulType().auraTimeout.cached()) {
                        extinguish();
                    }
                }

                private void extinguish() {
                    combatTargets.clear();
                    carrier.getWorld().spawnParticle(Particle.SMOKE, carrier.getLocation().add(0, 1, 0),
                            30, 0.5, 0.5, 0.5, 0.1);
                    carrier.getWorld().playSound(carrier, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 0.2f);

                    auraTask = null;
                    cancel();
                }
            }.runTaskTimer(SoulSnatcher.getPlugin(), 1L, 1L);
        }

        @Override
        protected void cleanUp() {
            if (this.auraTask != null)
                this.auraTask.cancel();
        }

        @Override
        protected void addCombatTarget(LivingEntity target) {
            super.addCombatTarget(target);

            if (this.auraTask == null)
                auraTask = createAuraTask();
        }
    }
}
