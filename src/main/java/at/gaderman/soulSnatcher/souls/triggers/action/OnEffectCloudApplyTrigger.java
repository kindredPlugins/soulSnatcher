package at.gaderman.soulSnatcher.souls.triggers.action;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

public interface OnEffectCloudApplyTrigger {
    void onEffectCloudApply(LivingEntity carrier, AreaEffectCloudApplyEvent event);
}
