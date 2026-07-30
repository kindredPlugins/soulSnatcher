package at.gaderman.soulSnatcher.souls.instances.combat;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@AutoService(SoulType.class)
public class HoglinSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<HoglinSoulType> create(LivingEntity carrier) {
        return new HoglinSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "hoglin_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.HOGLIN;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.COMBAT;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "7ad7b5aeb220c079e319cd70ac8800e80774a9313c22f38e77afb89999e6ec87";
    }

    @Override
    public @NotNull Component displayName() {
        return Component.text("Hoglin Soul", TextColor.color(0xe8a074));
    }

    @Override
    public @NotNull List<Component> description() {
        return ItemUtils.applyDefaultLoreStyle(
                Component.text((damagePreserveMultiplier.cached() * 100) + "% ", NamedTextColor.RED)
                                .append(Component.text("of damage taken from players/mobs is", NamedTextColor.WHITE)),
                Component.text("split into ", NamedTextColor.WHITE)
                        .append(Component.text(bufferSplitAmount.cached(), NamedTextColor.GOLD))
                        .append(Component.text(" portions taken in short intervals.", NamedTextColor.WHITE))
        );
    }

    //region Config Values

    private static final String DAMAGE_PRESERVE_CONFIG_ID = "damage_preserve_amount";
    private static final String BUFFER_SPLIT_CONFIG_ID = "buffer_split_amount";
    private static final String BUFFER_INTERVAL_CONFIG_ID = "buffer_interval";

    private final ConfigOption<Double> damagePreserveMultiplier = configOption(DAMAGE_PRESERVE_CONFIG_ID, 0.4, FileConfiguration::getDouble, value -> Math.clamp(value, 0, 1));
    private final ConfigOption<Integer> bufferSplitAmount = configOption(BUFFER_SPLIT_CONFIG_ID, 3, FileConfiguration::getInt, value -> Math.max(value, 0));
    private final ConfigOption<Integer> bufferInterval = configOption(BUFFER_INTERVAL_CONFIG_ID, 15, FileConfiguration::getInt, value -> Math.max(value, 0));

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                DAMAGE_PRESERVE_CONFIG_ID, "How much of damage taken is added preserve pool (in % => 0.4 = 40%)",
                BUFFER_SPLIT_CONFIG_ID, "In how many portions the damage is split off",
                BUFFER_INTERVAL_CONFIG_ID, "Time between each buffered damage portion is applied in ticks (20 ticks = 1 second)"
        );
    }

    //endregion

    public static class HoglinSoulInstance extends SoulInstance<HoglinSoulType> implements OnDamageReceivedTrigger {
        protected HoglinSoulInstance(LivingEntity carrier, HoglinSoulType soulType) {
            super(carrier, soulType);
        }

        private int currentCycle;
        private BukkitTask preserveApplicationTask;

        private boolean isDealingPreserveDamage;

        private final Map<Integer, List<BufferedDamage>> damageBuffer = new LinkedHashMap<>();

        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {
            if(isDealingPreserveDamage)
                return;

            double toPreserve = event.getDamage() * soulType().damagePreserveMultiplier.cached();
            event.setDamage(event.getDamage() - toPreserve);

            int splitAmount = soulType().bufferSplitAmount.cached();
            for (int i = 0; i < splitAmount; i++) {
                BufferedDamage bufferedDamage = new BufferedDamage(event.getDamageSource(), toPreserve / splitAmount);

                int index = currentCycle + i + 1;
                List<BufferedDamage> bufferedDamageList = this.damageBuffer.getOrDefault(index, new ArrayList<>());
                bufferedDamageList.add(bufferedDamage);
                this.damageBuffer.put(index, bufferedDamageList);
            }

            if(preserveApplicationTask == null)
                preserveApplicationTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(!carrier.isValid() || damageBuffer.isEmpty()){
                            cancel();
                            preserveApplicationTask = null;
                            return;
                        }

                        List<BufferedDamage> bufferedDamageList = damageBuffer.getOrDefault(currentCycle, Collections.emptyList());
                        damageBuffer.remove(currentCycle);

                        var knockbackRes = carrier.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                        double initialKnBkRes = knockbackRes == null ? 0 : knockbackRes.getBaseValue();
                        try {
                            if(knockbackRes != null)
                                knockbackRes.setBaseValue(1.0);

                            bufferedDamageList.forEach(bufferedDamage -> {
                                isDealingPreserveDamage = true;

                                int noDamageTicks = carrier.getNoDamageTicks();
                                carrier.setNoDamageTicks(0);
                                carrier.damage(bufferedDamage.damage, bufferedDamage.source);
                                carrier.setNoDamageTicks(noDamageTicks);

                                isDealingPreserveDamage = false;
                            });

                        }finally {
                            if(knockbackRes != null)
                                knockbackRes.setBaseValue(initialKnBkRes);
                        }

                        carrier.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, carrier.getEyeLocation(), 2,
                                0.2, 0.2, 0.2, 0.1, Material.REDSTONE_BLOCK.createBlockData());
                        carrier.getWorld().playSound(carrier, Sound.ENTITY_HOGLIN_HURT, 0.5f, 0.2f);

                        currentCycle++;
                    }
                }.runTaskTimer(SoulSnatcher.getPlugin(), 0L, soulType().bufferInterval.cached());
        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
        }

        private record BufferedDamage(DamageSource source, double damage) {}

        @Override
        protected void cleanUp() {
            LivingEntity carrier = carrier();

            if(carrier.isValid()) {
                if (!damageBuffer.isEmpty()) {
                    isDealingPreserveDamage = true;
                    damageBuffer.values().stream().flatMap(List::stream).forEach(bufferedDamage -> {
                        int noDamageTicks = carrier.getNoDamageTicks();
                        carrier.setNoDamageTicks(0);
                        carrier.damage(bufferedDamage.damage, bufferedDamage.source);
                        carrier.setNoDamageTicks(noDamageTicks);
                    });
                    isDealingPreserveDamage = false;
                }
                damageBuffer.clear();
            }
        }
    }
}
