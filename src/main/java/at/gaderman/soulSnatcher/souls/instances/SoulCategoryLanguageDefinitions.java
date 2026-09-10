package at.gaderman.soulSnatcher.souls.instances;

import at.gaderman.soulSnatcher.config.lang.LanguageGroupDefinition;
import at.gaderman.soulSnatcher.config.lang.LanguageKey;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;

@AutoService(LanguageGroupDefinition.class)
public class SoulCategoryLanguageDefinitions extends LanguageGroupDefinition {

    public static final String PREFIX = "soul_categories.";

    public static final LanguageKey COMBAT_TITLE = registerKey(PREFIX + "combat_title",
            Component.text("Combat"));
    public static final LanguageKey COMBAT_DESCRIPTION = registerKey(PREFIX + "combat_description",
            Component.text("Souls which directly aid combat prowess"));

    public static final LanguageKey UTILITY_TITLE = registerKey(PREFIX + "utility_title",
            Component.text("Utility"));
    public static final LanguageKey UTILITY_DESCRIPTION = registerKey(PREFIX + "utility_description",
            Component.text("Souls which have vast effects on gameplay"));

    public static final LanguageKey MOVEMENT_TITLE = registerKey(PREFIX + "movement_title",
            Component.text("Movement"));
    public static final LanguageKey MOVEMENT_DESCRIPTION = registerKey(PREFIX + "movement_description",
            Component.text("Souls which boost your movement capabilities"));

    public static final LanguageKey ATTRIBUTES_TITLE = registerKey(PREFIX + "attributes_title",
            Component.text("Attributes"));
    public static final LanguageKey ATTRIBUTES_DESCRIPTION = registerKey(PREFIX + "attributes_description",
            Component.text("Souls which change attributes of yourself"));
}
