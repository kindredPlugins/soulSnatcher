package at.gaderman.soulSnatcher.souls.triggers.action;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;

public interface OnPotionSplashTrigger {
    void onPotionSplash(LivingEntity carrier, ThrownPotion potion, PotionSplashEvent event);
}
