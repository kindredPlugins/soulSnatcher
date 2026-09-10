package at.gaderman.soulSnatcher.gui.menus;

import at.gaderman.soulSnatcher.config.lang.LanguageGroupDefinition;
import at.gaderman.soulSnatcher.config.lang.LanguageKey;
import com.google.auto.service.AutoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

@AutoService(LanguageGroupDefinition.class)
public class MenuLanguageDefinition extends LanguageGroupDefinition {

    public static final String SOUL_INDEX_PREFIX = "soul_index_menu.";
    public static final String SOUL_PLACEHOLDER = "{soul}";

    public static final LanguageKey GAMEPLAY_INFO_TITLE = registerKey(SOUL_INDEX_PREFIX + "gameplay_info.title",
            Component.text("Gameplay Info", NamedTextColor.GOLD));

    public static final LanguageKey GAMEPLAY_INFO_DESCRIPTION = registerKey(SOUL_INDEX_PREFIX + "gameplay_info.description",
            Component.text("View general information about how"),
            Component.text("this plugin operates.")
    );


    public static final String GAMEPLAY_INFO_PREFIX = SOUL_INDEX_PREFIX + "submenus.gameplay_info.";

    public static final LanguageKey RELEASING_SOULS = registerKey(GAMEPLAY_INFO_PREFIX + "releasing_souls",
            Component.text("Releasing Souls", NamedTextColor.YELLOW));
    public static final LanguageKey RELEASING_SOULS_DESCRIPTION = registerKey(GAMEPLAY_INFO_PREFIX + "releasing_souls_description",
            Component.text("After slaying a mob a ")
                    .append(Component.text("soul ", NamedTextColor.BLUE)),
            Component.text("is released and added to your pool."));

    public static final LanguageKey INFUSION = registerKey(GAMEPLAY_INFO_PREFIX + "infusion",
            Component.text("Infusion", NamedTextColor.YELLOW));
    public static final LanguageKey INFUSION_DESCRIPTION = registerKey(GAMEPLAY_INFO_PREFIX + "infusion_description",
            Component.text("Nearby spawned mobs take a ")
                    .append(Component.text("soul ", NamedTextColor.BLUE)),
            Component.text("from your pool and ")
                    .append(Component.text("infuse", NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.WHITE)),
            Component.text("They gain additional mechanics based"),
            Component.text("on the ")
                    .append(Component.text("soul ", NamedTextColor.BLUE))
                    .append(Component.text("they infused with.", NamedTextColor.WHITE))
    );

    public static final LanguageKey BINDING = registerKey(GAMEPLAY_INFO_PREFIX + "binding",
            Component.text("Binding", NamedTextColor.YELLOW));
    public static final LanguageKey BINDING_DESCRIPTION = registerKey(GAMEPLAY_INFO_PREFIX + "binding_description",
            Component.text("After killing an ")
                    .append(Component.text("infused ", NamedTextColor.YELLOW)),
            Component.text("mob, their infused ")
                    .append(Component.text("soul ", NamedTextColor.BLUE))
                    .append(Component.text("will be offered.", NamedTextColor.WHITE)),
            Component.text("Such ")
                    .append(Component.text("soul ", NamedTextColor.BLUE))
                    .append(Component.text("can be absorbed to gain", NamedTextColor.WHITE)),
            Component.text("additional mechanics for yourself.")
    );

    public static final LanguageKey VIAL = registerKey(GAMEPLAY_INFO_PREFIX + "vial",
            Component.text("Soul Vial", NamedTextColor.YELLOW));
    public static final LanguageKey VIAL_DESCRIPTION = registerKey(GAMEPLAY_INFO_PREFIX + "vial_description",
            Component.empty(),
            Component.text("Can be obtained through ")
                    .append(Component.text("Piglin Bartering", NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.WHITE))
    );

    public static final String SOUL_LANTERN_PREFIX = SOUL_INDEX_PREFIX + "submenus.soul_lantern.";

    public static final LanguageKey SOUL_LANTERN_TITLE = registerKey(SOUL_LANTERN_PREFIX + "title",
            Component.text("Soul Lantern", NamedTextColor.BLUE));

    public static final LanguageKey REMOVE_SOUL = registerKey(SOUL_LANTERN_PREFIX + "remove",
            Component.text("Remove " + SOUL_PLACEHOLDER, NamedTextColor.DARK_RED));
    public static final LanguageKey REMOVE_SOUL_DESC = registerKey(SOUL_LANTERN_PREFIX + "remove_soul",
            Component.text("Completely ")
                    .append(Component.text("removes ", NamedTextColor.RED))
                    .append(Component.text("this soul from yourself", NamedTextColor.WHITE))
    );

    public static final LanguageKey NO_SOUL_SLOT = registerKey(SOUL_LANTERN_PREFIX + "no_soul_slot",
            Component.text("Soul Slot empty", NamedTextColor.GRAY));
    public static final LanguageKey NO_SOUL_SLOT_DESCRIPTION = registerKey(SOUL_LANTERN_PREFIX + "no_soul_slot_description",
            Component.text("Bind a soul by killing an infused mob")
    );

    public static final String SOUL_INDEX_PREFIX_MAIN = SOUL_INDEX_PREFIX + "main.";

    public static final LanguageKey SOUL_INDEX_TITLE = registerKey(SOUL_INDEX_PREFIX_MAIN + "title",
            Component.text("Soul Index", NamedTextColor.BLUE));

    public static final LanguageKey BACK_BUTTON = registerKey(SOUL_INDEX_PREFIX_MAIN + "back_button",
            Component.text("Back", NamedTextColor.RED));

    public static final String SOUL_ABSORPTION_PREFIX = SOUL_INDEX_PREFIX + "submneus.absorption.";

    public static final LanguageKey CHOOSE_SOUL = registerKey(SOUL_ABSORPTION_PREFIX + "title",
            Component.text("Choose a soul"));
    public static final LanguageKey DISCARD_SOUL = registerKey(SOUL_ABSORPTION_PREFIX + "discard_soul",
            Component.text("Discard " + SOUL_PLACEHOLDER, NamedTextColor.RED));
    public static final LanguageKey REPLACE_SOUL = registerKey(SOUL_ABSORPTION_PREFIX + "replace_soul",
            Component.text("Replace " + SOUL_PLACEHOLDER, TextColor.color(0xd38531)));

}
