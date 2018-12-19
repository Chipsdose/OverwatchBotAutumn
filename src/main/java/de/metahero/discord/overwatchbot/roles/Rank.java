package de.metahero.discord.overwatchbot.roles;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Rank {

    GRANDMASTER(4000, "Grandmaster", "Grandmaster"),

    MASTER(3500, "Master", "Master"),

    DIAMOND(3000, "Diamond", "Diamond"),

    PLATIN(2500, "Platin", "Platin"),

    GOLD(2000, "Gold", "Gold"),

    SILVER(1500, "Silver", "Silver"),

    BRONZE(0, "Bronze", "Bronze"),

    UNKNOWN(Integer.MIN_VALUE, ":question:", "question"),

    ;

    private int minimumRating;
    private String discordEmote;
    private String discordRole;

    public static Rank fromRating(int rating) {
        return Arrays.stream(Rank.values()).filter(rank -> rating >= rank.getMinimumRating()).findFirst().orElse(UNKNOWN);
    }

    public static Rank fromRole(String role) {
        return Arrays.stream(Rank.values()).filter(f -> compare(f, role)).findFirst().orElse(UNKNOWN);
    }

    public static boolean isRole(String role) {
        return !fromRole(role).equals(UNKNOWN);
    }

    private static boolean compare(Rank rank, String role) {
        return rank.getDiscordRole().equalsIgnoreCase(role);
    }
}
