package at.gaderman.soulSnatcher.souls.items;

import at.gaderman.soulSnatcher.config.lang.LanguageGroupDefinition;
import at.gaderman.soulSnatcher.config.lang.LanguageKey;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@AutoService(LanguageGroupDefinition.class)
public class SoulItemsLanguageDefinitions extends LanguageGroupDefinition {

    public static final String PREFIX = "soul_items.";

    public static final String LANTERN_PREFIX = PREFIX + "soul_lantern.";
    public static final LanguageKey LANTERN_TITLE = registerKey(LANTERN_PREFIX + "title",
            "Soul Lantern");
    public static final LanguageKey LANTERN_DESCRIPTION = registerKey(LANTERN_PREFIX + "description",
            "Interact to open souls GUI",
            "",
            "Active souls:");
    public static final LanguageKey FIRST_SOUL_MESSAGE = registerKey(LANTERN_PREFIX + "first_soul_message",
            Component.text("------------- ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("SoulSnatcher", NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD))
                    .append(Component.text(" -------------", NamedTextColor.DARK_GRAY)),
            Component.text("You just received a ", NamedTextColor.WHITE)
                    .append(LANTERN_TITLE.getSingle())
                    .append(Component.text(".", NamedTextColor.WHITE)),
            Component.text("SoulSnatcher adds souls which add new mechanics, use this lantern to check yours out.", NamedTextColor.WHITE),
            Component.text("Use ", NamedTextColor.WHITE)
                    .append(Component.text("/soulindex ", NamedTextColor.AQUA))
                    .append(Component.text("to check out all souls and general information.", NamedTextColor.WHITE)));
    public static final LanguageKey LANTERN_DISPOSE = registerKey(LANTERN_PREFIX + "dispose",
            Component.text("You disposed your ", NamedTextColor.GRAY)
                    .append(LANTERN_TITLE.getSingle())
                    .append(Component.text("! Get a new one by running ", NamedTextColor.GRAY))
                    .append(Component.text("/soullantern", NamedTextColor.AQUA)));
    public static final LanguageKey EMPTY_UPDATE = registerKey(LANTERN_PREFIX + "empty_update",
            Component.text("You do not own any souls, so no soul lantern was added"));
    public static final LanguageKey LANTERN_UPDATE = registerKey(LANTERN_PREFIX + "update_message",
            Component.text("Your soul lantern has been updated!"));

    public static final String VIAL_PREFIX = PREFIX + "soul_vial.";
    public static final LanguageKey EMPTY_VIAL_TITLE = registerKey(VIAL_PREFIX + "empty_title",
            "Empty Soul Vial");
    public static final LanguageKey EMPTY_VIAL_DESCRIPTION = registerKey(VIAL_PREFIX + "empty_description",
            Component.text("Interact with an released soul to capture it.", NamedTextColor.GRAY),
            Component.text("Can later be released again.", NamedTextColor.GRAY));
    public static final LanguageKey FILLED_VIAL_PREFIX = registerKey(VIAL_PREFIX + "filled_prefix",
            "Soul Vial ✦ ");
    public static final LanguageKey FILLED_VIAL_DESCRIPTION_HEADER = registerKey(VIAL_PREFIX + "filled_description_header",
            Component.text("Interact to release the stored soul"));
    public static final LanguageKey VIAL_CAPTURE_DROPPED = registerKey(VIAL_PREFIX + "capture_dropped",
            "Your captured ");
    public static final LanguageKey VIAL_CAPTURE_DROPPED_SUFFIX = registerKey(VIAL_PREFIX + "capture_dropped_suffix",
            " has been dropped!");
    public static final LanguageKey VIAL_ALREADY_HAVE = registerKey(VIAL_PREFIX + "already_have",
            "You already have bound this soul");
    public static final LanguageKey VIAL_DISABLED = registerKey(VIAL_PREFIX + "disabled",
            "This soul has been disabled by an admin");

}
