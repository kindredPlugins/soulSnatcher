package at.gaderman.soulSnatcher.souls.effects;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class SoulReward {

    public static final NamespacedKey SOUL_REWARD = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_reward");
    public static final NamespacedKey REWARD_OWNER = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_reward_owner");
    public static final NamespacedKey HIDDEN_FOR = new NamespacedKey(SoulSnatcher.getPlugin(), "soul_reward_hidden_for_owner");

    public static void offerSoulReward(Location location, Player owner, SoulType soulType){
        location.setPitch(0);

        boolean duplicateSoul = !soulType.canOverwriteItself() && SoulType.getCarriedSouls(owner).stream().anyMatch(soul -> soul.soulType().id().equals(soulType.id()));

        location.getWorld().playSound(location, Sound.BLOCK_LEVER_CLICK, 1f, 0.5f);
        location.getWorld().playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 0.5f);

        ItemDisplay skullDisplay = location.getWorld().spawn(location.clone().add(0, 1.5, 0), ItemDisplay.class, display -> {
            display.setItemStack(soulType.getRepresentativeSkull());

            Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.PI, 0, 1, 0),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transformation);
            display.setBillboard(Display.Billboard.VERTICAL);
        });
        TextDisplay soulTitle = location.getWorld().spawn(skullDisplay.getLocation().clone().add(0, 0.15, 0), TextDisplay.class, display -> {
            display.text(soulType.displayName());
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.VERTICAL);
        });
        TextDisplay claimedByTitle = location.getWorld().spawn(skullDisplay.getLocation().clone().add(0, 0.15, 0), TextDisplay.class, display -> {
            display.text(Component.text("Offered for ", NamedTextColor.RED)
                    .append(owner.name().color(NamedTextColor.DARK_RED)));
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.VERTICAL);

            display.getPersistentDataContainer().set(HIDDEN_FOR, PersistentDataType.STRING, owner.getUniqueId().toString());
            owner.hideEntity(SoulSnatcher.getPlugin(), display);
        });
        TextDisplay interactText = location.getWorld().spawn(skullDisplay.getLocation().clone().add(0, -1, 0), TextDisplay.class, display -> {
            display.text(duplicateSoul ?
                    Component.text("Already bound this soul", NamedTextColor.RED) :
                    Component.keybind("key.use", NamedTextColor.YELLOW).append(Component.text(" to bind")));

            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.VERTICAL);
        });
        Interaction soulInteraction = location.getWorld().spawn(location.clone().add(0, 0.4, 0), Interaction.class, interaction -> {
            interaction.setInteractionHeight(1.5f);
            interaction.setInteractionWidth(1.0f);

            interaction.setVisibleByDefault(false);
            owner.showEntity(SoulSnatcher.getPlugin(), interaction);

            interaction.getPersistentDataContainer().set(REWARD_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
            interaction.getPersistentDataContainer().set(SOUL_REWARD, PersistentDataType.STRING, soulType.id());
        });
        List<Entity> displayEntities = List.of(skullDisplay, soulTitle, interactText, claimedByTitle);
        displayEntities.forEach(entity -> {
            if (!entity.equals(skullDisplay) && !entity.equals(claimedByTitle)) {
                entity.setVisibleByDefault(false);
                owner.showEntity(SoulSnatcher.getPlugin(), entity);
            }

            entity.getPersistentDataContainer().set(SOUL_REWARD, PersistentDataType.STRING, soulInteraction.getUniqueId().toString());
            entity.getPersistentDataContainer().set(REWARD_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        });

        long age = duplicateSoul ? 80L : 60 * 20L;

        soulInteraction.getWorld().playSound(soulInteraction.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.25f);
        SoulSnatcher.getPlugin().registerDelayedTask(() -> {
            if (soulInteraction.isDead()) return;

            soulInteraction.remove();
            displayEntities.forEach(Entity::remove);

            SoulEffects.discardSoulRewardEffect(soulInteraction.getLocation());
        }, age);
    }

    /**
     * Removes a soul reward (meaning all related entities) based on the given interaction entity that is the core of the reward
     *
     * @param rewardTrigger The interaction which acts as core for the reward
     */
    public static void removeSoulReward(Interaction rewardTrigger) {
        rewardTrigger.remove();
        Stream.of(
                        rewardTrigger.getWorld().getNearbyEntitiesByType(TextDisplay.class, rewardTrigger.getLocation(), 2),
                        rewardTrigger.getWorld().getNearbyEntitiesByType(ItemDisplay.class, rewardTrigger.getLocation(), 2)
                )
                .flatMap(Collection::stream)
                .filter(display -> display.getPersistentDataContainer()
                        .getOrDefault(SOUL_REWARD, PersistentDataType.STRING, "")
                        .equals(rewardTrigger.getUniqueId().toString()))
                .forEach(Entity::remove);
    }

}
