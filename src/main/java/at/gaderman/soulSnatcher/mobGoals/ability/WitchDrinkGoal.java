package at.gaderman.soulSnatcher.mobGoals.ability;

import at.gaderman.soulSnatcher.SoulSnatcher;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.EnumSet;

public class WitchDrinkGoal extends SoulAbilityGoal{
    private long lasDrink;

    public WitchDrinkGoal(Mob mob){
        super(mob);
    }

    public static final GoalKey<@NotNull Mob> GOAL_KEY = GoalKey.of(Mob.class, new NamespacedKey(SoulSnatcher.getPlugin(),
            "infused_witch_drinking"));

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GOAL_KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }

    private static final long DRINK_COOLDOWN = 5000;

    @Override
    public boolean shouldActivate() {
        return  lasDrink < System.currentTimeMillis() - DRINK_COOLDOWN &&
                (mob.getTarget() != null || mob.getHealth() < mob.getAttribute(Attribute.MAX_HEALTH).getValue());
    }

    private long drinkingTicks;
    private static final long DRINK_DURATION = 20L;

    @Override
    public boolean shouldStayActive() {
        return drinkingTicks <= DRINK_DURATION;
    }

    private ItemDisplay potion;
    private PotionType effectType;

    private static final NamespacedKey DRINKIN_MOD = new NamespacedKey(SoulSnatcher.getPlugin(), "drinking_potion");

    @Override
    public void start() {
        chooseEffectType();
        if(effectType == null) return;

        mob.setAggressive(false);

        Location potSource = mob.getLocation().add(0, mob.getEyeHeight(true), 0)
                .add(mob.getLocation().getDirection().normalize().setY(0).multiply(0.5));
        potSource.getWorld().spawnParticle(Particle.WITCH, potSource, 10);
        potSource.getWorld().playSound(potSource, Sound.ENTITY_WITCH_DRINK, 1f, 0.75f);

        potion = mob.getWorld().spawn(potSource, ItemDisplay.class, display -> {
            display.setItemStack(createPotion());
            display.setBillboard(Display.Billboard.FIXED);

            display.setInterpolationDuration(1);
            display.setInterpolationDelay(1);

            Transformation transformation =
                    new Transformation(
                            new Vector3f(),
                            new AxisAngle4f(),
                            new Vector3f(0.8f,0.8f,0.8f),
                            new AxisAngle4f()
                    );

            display.setTransformation(transformation);
        });

        mob.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier(DRINKIN_MOD, -0.2, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        SoulSnatcher.getPlugin().registerDelayedTask(potion::remove, 2 * DRINK_DURATION + 1);
    }

    @Override
    public void tick() {
        if (!mob.isValid()) {
            potion.remove();
            return;
        }

        Location loc = mob.getLocation()
                .add(0, mob.getEyeHeight(true) - 0.2, 0)
                .add(
                        mob.getLocation()
                                .getDirection()
                                .setY(0)
                                .normalize()
                                .multiply(0.45)
                );
        loc.add(0, Math.sin(drinkingTicks) * 0.075, 0);

        potion.teleport(loc);

        drinkingTicks++;
    }

    @Override
    public void stop() {
        drinkingTicks = 0;
        lasDrink = System.currentTimeMillis();


        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1f, 1f);
        mob.getWorld().spawnParticle(Particle.ENTITY_EFFECT, mob.getEyeLocation(), 30, 0, 0.3, 0,
                1, Color.GRAY);

        if(effectType != null)
            effectType.getPotionEffects().forEach(mob::addPotionEffect);

        mob.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(DRINKIN_MOD);

        if(mob.getTarget() != null)
            mob.setAggressive(true);
    }

    private void chooseEffectType(){
        if(mob.getFireTicks() > 0 && mob.getActivePotionEffects().stream().noneMatch(effect -> effect.getType() == PotionEffectType.FIRE_RESISTANCE))
            effectType = PotionType.FIRE_RESISTANCE;

        else if(mob.getRemainingAir() == 0 && mob.getActivePotionEffects().stream().noneMatch(effect -> effect.getType() == PotionEffectType.WATER_BREATHING))
            effectType = PotionType.WATER_BREATHING;

        else if(mob.getHealth() < mob.getAttribute(Attribute.MAX_HEALTH).getValue())
            effectType = Tag.ENTITY_TYPES_UNDEAD.isTagged(mob.getType()) ? PotionType.HARMING : PotionType.HEALING;

        else if(mob.getActivePotionEffects().stream().noneMatch(effect -> effect.getType() == PotionEffectType.SPEED))
            effectType = PotionType.SWIFTNESS;

        else effectType = null;
    }

    private ItemStack createPotion(){
        ItemStack potion = ItemStack.of(Material.POTION);
        potion.editMeta(meta -> ((PotionMeta) meta).setBasePotionType(effectType));
        return potion;
    }
}
