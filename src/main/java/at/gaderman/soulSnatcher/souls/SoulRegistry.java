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

    private final Map<String, SoulType> soulRegistry = new LinkedHashMap<>();

    private SoulRegistry() {
        ServiceLoader<SoulType> loader = ServiceLoader.load(SoulType.class, getClass().getClassLoader());
        for(SoulType soulType : loader){
            soulRegistry.put(soulType.id(), soulType);
        }
    }

    public @Nullable SoulType getSoul(String id){
        return soulRegistry.get(id);
    }
    public Optional<SoulType> getSoul(EntityType entityType){
        return soulRegistry.values().stream().filter(soul -> soul.entityType().equals(entityType)).findFirst();
    }
    public Map<String, SoulType> soulRegistryMap(){ return new LinkedHashMap<>(soulRegistry); }
}
