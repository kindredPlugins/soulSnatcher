package at.gaderman.soulSnatcher.souls;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class SoulRegistry {

    private static SoulRegistry instance;

    public static SoulRegistry getInstance(){
        if(instance == null) instance = new SoulRegistry();
        return instance;
    }

    private final Map<String, Soul> soulRegistry = new LinkedHashMap<>();

    private SoulRegistry() {
        ServiceLoader<Soul> loader = ServiceLoader.load(Soul.class, getClass().getClassLoader());
        for(Soul soul : loader){
            soulRegistry.put(soul.id(), soul);
        }
    }

    public @Nullable Soul getSoul(String id){
        return soulRegistry.get(id);
    }
    public Optional<Soul> getSoul(EntityType entityType){
        return soulRegistry.values().stream().filter(soul -> soul.entityType().equals(entityType)).findFirst();
    }
    public Map<String, Soul> soulRegistryMap(){ return new LinkedHashMap<>(soulRegistry); }
}
