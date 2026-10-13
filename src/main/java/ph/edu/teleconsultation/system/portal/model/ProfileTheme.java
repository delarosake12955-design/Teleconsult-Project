package ph.edu.teleconsultation.system.portal.model;

import java.util.List;

/**
 * A colour theme for a user's profile header and avatar. The first theme
 * (Ocean Blue) is the system default.
 */
public record ProfileTheme(String key, String name,
                           String from, String mid, String to, String text) {

    public static final String DEFAULT_KEY = "blue";

    private static final List<ProfileTheme> ALL = List.of(
            new ProfileTheme("blue",    "Ocean Blue", "#1d4ed8", "#3478f6", "#5aa2ff", "#1d4ed8"),
            new ProfileTheme("indigo",  "Indigo",     "#3730a3", "#5b5ef0", "#8b8dfb", "#4338ca"),
            new ProfileTheme("cyan",    "Lagoon",     "#0e7490", "#0ea5c6", "#5fd4ea", "#0e7490"),
            new ProfileTheme("violet",  "Violet",     "#5b21b6", "#7a5af8", "#a78bfa", "#6d28d9"),
            new ProfileTheme("emerald", "Emerald",    "#047857", "#10b981", "#5ee0b0", "#047857"),
            new ProfileTheme("rose",    "Rose",       "#be123c", "#f43f5e", "#fb8fa3", "#be123c"),
            new ProfileTheme("amber",   "Sunrise",    "#b45309", "#f59e0b", "#fcd34d", "#b45309"),
            new ProfileTheme("slate",   "Midnight",   "#1e293b", "#475569", "#94a3b8", "#334155"));

    public static List<ProfileTheme> all() {
        return ALL;
    }

    /** The theme with this key, or the default one if the key is unknown. */
    public static ProfileTheme byKey(String key) {

        for (ProfileTheme theme : ALL) {
            if (theme.key().equals(key)) {
                return theme;
            }
        }

        return ALL.get(0);
    }

    /** JavaFX CSS paint for the header background. */
    public String gradientCss() {
        return "linear-gradient(to right, " + from + " 0%, " + mid + " 55%, " + to + " 100%)";
    }
}
