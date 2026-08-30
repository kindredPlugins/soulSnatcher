package at.gaderman.soulSnatcher.souls.triggers.input;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.jetbrains.annotations.NotNull;

public interface OnEntityToggleGlideTrigger {
    void onToggleGlide(LivingEntity carrier, @NotNull EntityToggleGlideEvent event);
}
