package de.metahero.discord.overwatchbot.roles;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TeamRole {

    TANK("Tank", "Tank"),

    SUPPORT("Support", "Support"),

    DAMAGE("Damage", "Damage"),

    FLEX("Flex", "Flex"),

    STAFF("Staff", "Staff"),

    UNKNOWN(":question:", "question"),

    ;
    private String discordEmote;

    private String discordRole;

    public static TeamRole fromRole(String role) {
        return Arrays.stream(TeamRole.values()).filter(f -> compare(f, role)).findFirst().orElse(UNKNOWN);
    }

    public static boolean isRole(String role) {
        return !fromRole(role).equals(UNKNOWN);
    }

    private static boolean compare(TeamRole flag, String role) {
        return flag.getDiscordRole().equalsIgnoreCase(role);
    }
}
