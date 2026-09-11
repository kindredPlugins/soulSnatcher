package at.gaderman.soulSnatcher.souls;

import at.gaderman.soulSnatcher.config.lang.LanguageGroupDefinition;
import at.gaderman.soulSnatcher.config.lang.LanguageKey;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@AutoService(LanguageGroupDefinition.class)
public class SoulLanguageDefinitions extends LanguageGroupDefinition {

    public static final String PREFIX = "general.";

    public static final String SOUL_PLACEHOLDER = "{soul}";
    public static final LanguageKey SOUL_NAME = registerKey(PREFIX + "soul",
            Component.text(SOUL_PLACEHOLDER + " Soul"));

    public static final LanguageKey BIND_TEXT = registerKey(PREFIX + "bind_text",
            Component.keybind("key.use", NamedTextColor.YELLOW).append(Component.text(" to bind")));
    public static final LanguageKey ALREADY_BOUND = registerKey(PREFIX + "soul_already_bound",
            Component.text("Already bound this soul"));

    public static final String OWNER_PLACEHOLDER = "{owner}";
    public static final LanguageKey SOUL_OFFERED_FOR = registerKey(PREFIX + "offered_for",
            Component.text("Offered for " + OWNER_PLACEHOLDER));

    public static final String VIAL_PLACEHOLDER = "{soul_vial}";
    public static final LanguageKey DISABLED_WHILE_OFFLINE = registerKey(PREFIX + "disabled_while_offline",
            Component.text("Souls you had bound were ")
                    .append(Component.text("disabled ", NamedTextColor.RED))
                    .append(Component.text("while you were offline", NamedTextColor.WHITE)));
    public static final LanguageKey RECEIVED_AS_SOUL_VIAL = registerKey(PREFIX + "disabled_received_vial",
            Component.text("You have received them as " + VIAL_PLACEHOLDER));
    public static final LanguageKey VIAL_DROP_FULL_INV = registerKey(PREFIX + "dropped_vials",
            Component.text("Some vials have been dropped due to full inventory!"));

    public static final LanguageKey DISABLED_IN_WORLD = registerKey(PREFIX + "disabled_in_world",
            Component.text("Souls have been disabled in this world"));
    public static final LanguageKey ENABLED_IN_WORLD = registerKey(PREFIX + "enabled_in_world",
            Component.text("Souls have been enabled in this world"));

    public static final String MAX_SOUL_PLACEHOLDER = "{max_souls}";
    public static final LanguageKey MAX_SOUL_REPLACEMENT = registerKey(PREFIX + "max_soul_replacement",
            Component.text("You received " + SOUL_PLACEHOLDER + " as vial since max bound souls were reduced to " + MAX_SOUL_PLACEHOLDER));

}
