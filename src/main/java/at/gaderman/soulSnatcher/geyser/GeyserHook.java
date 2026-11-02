package at.gaderman.soulSnatcher.geyser;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulRegistry;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;

import java.util.UUID;

final public class GeyserHook implements EventRegistrar {
    static boolean isBedrockPlayer(UUID uuid) {
        return GeyserApi.api().isBedrockPlayer(uuid);
    }

    void register(SoulSnatcher plugin) {
        GeyserApi.api().eventBus().subscribe(this, GeyserDefineCustomSkullsEvent.class, this::onCustomSkullsRegister);

        plugin.getLogger().info("Geyser setup complete");
    }

    public void onCustomSkullsRegister(GeyserDefineCustomSkullsEvent event) {
        SoulSnatcher.getPlugin().getLogger().info("Registering custom skulls for geyser");

        SoulRegistry.getInstance().soulRegistryMap().values().forEach(soul ->
                event.register(soul.skullTexture(), GeyserDefineCustomSkullsEvent.SkullTextureType.SKIN_HASH)
        );

        SoulSnatcher.getPlugin().getLogger().info("Register custom geyser skulls done");
    }
}
