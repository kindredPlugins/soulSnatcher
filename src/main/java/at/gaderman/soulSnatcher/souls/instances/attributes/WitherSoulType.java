package at.gaderman.soulSnatcher.souls.instances.attributes;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulInstance;
import at.gaderman.soulSnatcher.souls.SoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigHoldingSoulType;
import at.gaderman.soulSnatcher.souls.config.ConfigOption;
import at.gaderman.soulSnatcher.souls.instances.AttributeSoul;
import at.gaderman.soulSnatcher.souls.instances.SoulCategory;
import at.gaderman.soulSnatcher.souls.triggers.OnEntityPotionEffectTrigger;
import at.gaderman.soulSnatcher.souls.triggers.OnRegainHealthTrigger;
import at.gaderman.soulSnatcher.souls.triggers.damage.OnDamageReceivedTrigger;
import at.gaderman.soulSnatcher.utils.ItemUtils;
import com.google.auto.service.AutoService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@AutoService(SoulType.class)
public class WitherSoulType extends ConfigHoldingSoulType {

    @Override
    public @NotNull SoulInstance<WitherSoulType> create(LivingEntity carrier) {
        return new WitherSoulInstance(carrier, this);
    }

    @Override
    public @NotNull String id() {
        return "wither_soul";
    }

    @Override
    public @NotNull EntityType entityType() {
        return EntityType.WITHER;
    }

    @Override
    public @NotNull SoulCategory category() {
        return SoulCategory.ATTRIBUTES;
    }

    @Override
    protected @NotNull String skullTexture() {
        return "63b6bb53e33db2c19ae88b5ce7e24e8b5f3137c411b4f704f0aebd5deee15694";
    }

    @Override
    public @NotNull Component displayName() {
        return ItemUtils.gradient("Wither Soul", TextColor.color(0xb4b4b4), TextColor.color(0x414141));
    }

    @Override
    public @NotNull List<Component> description() {
        Component menuArrow = Component.text("➤ ", NamedTextColor.GRAY);

        return ItemUtils.applyDefaultLoreStyle(
            Component.text("Become undead:", NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD),
                menuArrow.append(Component.text("Reversed instant health & damage", NamedTextColor.GRAY)),
                menuArrow.append(Component.text("Immunity to: Regeneration, Poison, Wither", NamedTextColor.GRAY)),
                menuArrow.append(Component.text("Susceptible to Smite", NamedTextColor.GRAY)),
                menuArrow.append(Component.text("Passive Regen unrelated to hunger", NamedTextColor.GRAY)),
                menuArrow.append(Component.text("+" + attackDamageBonus.cached() + " Attack Damage", NamedTextColor.BLUE)),
                menuArrow.append(Component.text("+" + armorToughnessBonus.cached() + " Armor Toughness", NamedTextColor.BLUE))
        );
    }

    //region Config values

    private static final String PASSIVE_HEALTH_TICK = "passive_health_tick";
    private static final String PASSIVE_HEALTH_AMOUNT = "passive_health_amount";
    private static final String ATTACK_DAMAGE_BONUS = "attack_damage_bonus";
    private static final String ARMOR_THOUGHNESS_BONUS = "armor_toughness_bonus";

    private final ConfigOption<Integer> passiveHealthTick = configOption(PASSIVE_HEALTH_TICK, 5, FileConfiguration::getInt);
    private final ConfigOption<Double> passiveHealthAmount = configOption(PASSIVE_HEALTH_AMOUNT, 0.25, FileConfiguration::getDouble);
    private final ConfigOption<Double> attackDamageBonus = configOption(ATTACK_DAMAGE_BONUS, 1.5, FileConfiguration::getDouble);
    private final ConfigOption<Double> armorToughnessBonus = configOption(ARMOR_THOUGHNESS_BONUS, 3.0, FileConfiguration::getDouble);

    @Override
    public Map<String, String> extraConfigPathCommentMap() {
        return Map.of(
                PASSIVE_HEALTH_TICK, "The interval of the passive health regen healing in ticks (20 ticks = 1 second)",
                PASSIVE_HEALTH_AMOUNT, "The amount healed on each interval tick",
                ATTACK_DAMAGE_BONUS, "Passive flat base attack damage bonus",
                ARMOR_THOUGHNESS_BONUS, "Passive flat armor toughness bonus"
        );
    }

    //endregion

    public static class WitherSoulInstance extends AttributeSoul<WitherSoulType> implements OnEntityPotionEffectTrigger, OnDamageReceivedTrigger, OnRegainHealthTrigger {

        private ScheduledTask regenTask;

        protected WitherSoulInstance(LivingEntity carrier, WitherSoulType soulType) {
            super(carrier, soulType);

            immuneEffects.forEach(carrier::removePotionEffect);

            int healTicks = soulType.passiveHealthTick.cached();
            double healAmount = soulType.passiveHealthAmount.cached();

            AtomicLong tick = new AtomicLong();
            regenTask = carrier.getScheduler().runAtFixedRate(SoulSnatcher.getPlugin(), _ -> {
                double health = carrier.getHealth();

                var maxHealth = carrier.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null && maxHealth.getValue() > health) {
                    if(tick.get() % 100 == 0) {
                        spawnWitherParticles();
                        carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 1f);
                    }

                    tick.getAndIncrement();
                }

                carrier.heal(healAmount, EntityRegainHealthEvent.RegainReason.MAGIC_REGEN);
            }, null, healTicks, healTicks);
        }

        @Override
        public Map<Attribute, AttributeModifier> attributeModifierMap() {
            return Map.of(
                    Attribute.ATTACK_DAMAGE, createModifier(soulType().attackDamageBonus.cached(), AttributeModifier.Operation.ADD_NUMBER),
                    Attribute.ARMOR_TOUGHNESS, createModifier(soulType().armorToughnessBonus.cached(), AttributeModifier.Operation.ADD_NUMBER)
            );
        }

        private final List<PotionEffectType> immuneEffects = List.of(
                PotionEffectType.POISON,
                PotionEffectType.REGENERATION,
                PotionEffectType.WITHER
        );

        private void spawnWitherParticles(){
            carrier().getWorld().spawnParticle(Particle.LARGE_SMOKE, carrier().getEyeLocation(), 5, 0.5, 0.5, 0.5, 0);
        }

        //TODO: does not actually care for instant effects they simply bypass this event, needs complicated instant health and damage check
        @Override
        public void onEntityPotionEffect(LivingEntity carrier, EntityPotionEffectEvent event) {
            PotionEffect newEffect = event.getNewEffect();
            if (newEffect == null) return;

            if (immuneEffects.contains(newEffect.getType())) {
                event.setCancelled(true);
                return;
            }

            if (newEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
                event.setCancelled(true);

                DamageSource.Builder sourceBuilder = DamageSource.builder(DamageType.MAGIC);

                if (event.getSource() != null)
                    sourceBuilder.withDirectEntity(event.getSource());

                carrier.damage((newEffect.getAmplifier() + 1) * 4, sourceBuilder.build());
                return;
            }

            if (newEffect.getType() == PotionEffectType.INSTANT_DAMAGE) {
                event.setCancelled(true);

                carrier.heal((newEffect.getAmplifier() + 1) * 6, EntityRegainHealthEvent.RegainReason.MAGIC);
                return;
            }
        }


        @Override
        public void onDamageReceived(LivingEntity carrier, EntityDamageEvent event) {

        }

        @Override
        public void onDamageReceivedByEntity(LivingEntity carrier, LivingEntity damager, EntityDamageByEntityEvent event) {
            if (event.getDamageSource().isIndirect())
                return;

            EntityEquipment equipment = damager.getEquipment();
            if (equipment == null)
                return;

            int smiteLevel = equipment.getItemInMainHand().getEnchantmentLevel(Enchantment.SMITE);
            if (smiteLevel == 0)
                return;

            carrier.getWorld().playSound(carrier.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 1f);
            event.setDamage(event.getDamage() + (2.5 * smiteLevel));
        }

        @Override
        public void onRegainHealth(LivingEntity carrier, EntityRegainHealthEvent event) {
            if (event.isFastRegen() || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                    || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN
                    || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.EATING) {
                if (carrier instanceof Player player)
                    player.getScheduler().run(SoulSnatcher.getPlugin(), _ -> {
                        player.setExhaustion(Math.max(0, player.getExhaustion() - 6));
                    }, null);

                event.setCancelled(true);
            }
        }

        protected void cleanup(){
            regenTask.cancel();
            regenTask = null;
        }
    }
}
