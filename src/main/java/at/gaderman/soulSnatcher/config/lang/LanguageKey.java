package at.gaderman.soulSnatcher.config.lang;

import net.kyori.adventure.text.Component;

import java.util.List;

public class LanguageKey {
    private final String key;

    public LanguageKey(String key) {
        this.key = key;
    }

    public Component getSingle(){
        return LanguageManager.getInstance().resolveComponent(key).getFirst();
    }

    public List<Component> getLines(){
        return LanguageManager.getInstance().resolveComponent(key);
    }
}
