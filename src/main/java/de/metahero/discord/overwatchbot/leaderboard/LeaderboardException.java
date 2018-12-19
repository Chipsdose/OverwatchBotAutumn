package de.metahero.discord.overwatchbot.leaderboard;

public class LeaderboardException extends Exception {
    private static final long serialVersionUID = 4753334702751171961L;

    public LeaderboardException() {
        super();
    }

    public LeaderboardException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public LeaderboardException(String message, Throwable cause) {
        super(message, cause);
    }

    public LeaderboardException(String message) {
        super(message);
    }

    public LeaderboardException(Throwable cause) {
        super(cause);
    }

}
