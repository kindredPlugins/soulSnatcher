package at.gaderman.soulSnatcher.config.lang;

import at.gaderman.soulSnatcher.SoulSnatcher;
import at.gaderman.soulSnatcher.souls.SoulType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

public class LanguageManager {

    private static LanguageManager instance;

    private LanguageManager() {
        setUp();
    }

    public static LanguageManager getInstance() {
        if (instance == null) instance = new LanguageManager();
        return instance;
    }

    private final Map<String, List<Component>> translationKeyMap = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private void setUp() {
        //TODO: resolve current language/cache it from config.yaml
        loadLanguage("en");

        SoulSnatcher.getPlugin().getLogger().info("Loaded all language keys");
    }

    private void loadLanguage(String language) {
        File langFile = new File(SoulSnatcher.getPlugin().getDataFolder(), "lang/lang-" + language + ".yml");
        boolean create = !langFile.exists();

        if (create)
            SoulSnatcher.getPlugin().getLogger().warning("Language file " + language + " not found! Creating default one...");

        YamlConfiguration config = create ? new YamlConfiguration() : YamlConfiguration.loadConfiguration(langFile);

        ServiceLoader<SoulType> loader = ServiceLoader.load(SoulType.class, getClass().getClassLoader());
        StreamSupport.stream(loader.spliterator(), false)
                .sorted(Comparator.comparing(SoulType::id))
                .forEach(keyHolder -> keyHolder.languageKeyDefaultMap().forEach((key, value) -> {
                    if (!config.contains(key))
                        config.set(key, value.stream().map(miniMessage::serialize).toList());

                    List<String> rawComponents = config.getStringList(key);
                    List<Component> components = resolveComponents(rawComponents, keyHolder);

                    translationKeyMap.put(key, components);
                }));

        try {
            config.save(langFile);
        } catch (IOException exception) {
            translationKeyMap.clear();
            SoulSnatcher.getPlugin().getLogger().severe("Could not save language file " + langFile.getAbsolutePath());
        }
    }

    private List<Component> resolveComponents(List<String> rawComponents, LanguageKeyHolder keyHolder) {
        if (keyHolder instanceof LanguageKeyPlaceholderHolder placeholderHolder)
            rawComponents = rawComponents.stream().map(line -> {
                String filled = line;
                for (Map.Entry<String, String> entry : placeholderHolder.placeholderMap().entrySet())
                    filled = filled.replace(placeholderHolder.wrapPlaceholder(entry.getKey()), entry.getValue());
                return filled;
            }).toList();

        return rawComponents.stream()
                .map(miniMessage::deserialize)
                .toList();
    }

    public List<Component> resolveComponent(String key) {
        if (translationKeyMap.containsKey(key))
            return translationKeyMap.get(key);

        //TODO: resolve current language/cache it from config.yaml
        String language = "en";

        File langFile = new File(SoulSnatcher.getPlugin().getDataFolder(), "lang/lang-" + language + ".yml");

        if (!langFile.exists()) {
            SoulSnatcher.getPlugin().getLogger().warning("Language file " + language + " not found!");
            return List.of();
        }

        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        Component component = langConfig.getComponent(key, miniMessage);

        if (component == null) {
            SoulSnatcher.getPlugin().getLogger().warning("No translation key for \"" + key + "\" found!");
            return List.of();
        }

        translationKeyMap.put(key, List.of(component));
        return List.of(component);
    }

//    public Component loadSoulDescription(SoulType soulType) {
//        String id = soulType.id();
//
//        Component component = translations.getOrDefault(id, null);
//
//        if (component != null)
//            return component;
//
//        //TODO: pull line from lang.yml file or default to soulType itself
//        List<String> rawLines = new ArrayList<>();
//        String rawDesc = String.join("<newline>", rawLines);
//        component = miniMessage.deserialize(rawDesc);
//        translations.put(id, component);
//
//        return component;
//    }
}
