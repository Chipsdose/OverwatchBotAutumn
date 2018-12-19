package de.metahero.discord.overwatchbot.roles;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Flag {
    BADEN_WÜRTTEMBERG("BadenWuerttemberg", "Baden-Württemberg"),

    BAYERN("Bayern", "Bayern"),

    BERLIN("Berlin", "Berlin"),

    BRANDENBURG("Brandenburg", "Brandenburg"),

    BREMEN("Bremen", "Bremen"),

    HAMBURG("Hamburg", "Hamburg"),

    HESSEN("Hessen", "Hessen"),

    MECKLENBURG_VORPOMMERN("MecklenburgVorpommern", "Mecklenburg-Vorpommern"),

    NIEDERSACHSEN("Niedersachsen", "Niedersachsen"),

    NORDRHEIN_WESTFALEN("NordrheinWestfalen", "Nordrhein-Westfalen"),

    RHEINLAND_PFALZ("RheinlandPfalz", "Rheinland-Pfalz"),

    SAARLAND("Saarland", "Saarland"),

    SACHSEN("Sachsen", "Sachsen"),

    SACHSEN_ANHALT("SachsenAnhalt", "Sachsen-Anhalt"),

    SCHLESWIG_HOLSTEIN("SchleswigHolstein", "Schleswig-Holstein"),

    THÜRINGEN("Thueringen", "Thüringen"),

    ÖSTERREICH("Oesterreich", "Österreich"),

    BURGENLAND("Burgenland", "Burgenland"),

    KÄRNTEN("Kaernten", "Kärnten"),

    OBERÖSTERREICH("Oberoesterreich", "Oberösterreich"),

    NIEDERÖSTERREICH("Niederoesterreich", "Niederösterreich"),

    SALZBURG("Salzburg", "Salzburg"),

    STEIERMARK("Steiermark", "Steiermark"),

    TIROL("Tirol", "Tirol"),

    VORARLBERG("Vorarlberg", "Vorarlberg"),

    WIEN("Wien", "Wien"),

    SCHWEIZ(":flag_ch:", "Schweiz"),

    UNKNOWN(":question:", "question"),

    ;

    private String discordEmote;

    private String discordRole;

    public static Flag fromRole(String role) {
        return Arrays.stream(Flag.values()).filter(f -> compare(f, role)).findFirst().orElse(UNKNOWN);
    }

    public static boolean isRole(String role) {
        return !fromRole(role).equals(UNKNOWN);
    }

    private static boolean compare(Flag flag, String role) {
        return flag.getDiscordRole().equalsIgnoreCase(role);
    }
}
