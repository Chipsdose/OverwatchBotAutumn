package de.metahero.discord.overwatchbot.leaderboard;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.removePattern;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.core.entities.MessageEmbed;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class LeaderboardFactory {

    public static final String DEFAULT = "default";
    public static final String ODCM = "odcm";

    public static LeaderboardFactory newInstance(String type) {
        if (DEFAULT.equalsIgnoreCase(type)) {
            return new DefaultLeaderboardFactory();
        }
        if (ODCM.equalsIgnoreCase(type)) {
            return new ODCMLeaderboardFactory();
        }
        throw new IllegalArgumentException(format("Type[%s] is not a LeaderboardFactory type", type));
    }

    protected abstract String createLeaderboardEntry(LeaderboardPlayer player);

    public List<MessageEmbed> createLeaderboard(List<LeaderboardPlayer> players) {
        final List<MessageEmbed> embeds = new ArrayList<>();
        embeds.add(this.createLeaderBoardHeader());
        final List<LeaderboardPlayer> validPlayers = players.stream().filter(p -> p.getRating() > 0).collect(Collectors.toList());
        final List<LeaderboardPlayer> unrankedPlayers = ListUtils.removeAll(players, validPlayers);
        final boolean onlyValidPlayers = unrankedPlayers.isEmpty();
        embeds.addAll(this.createBoard("Current Ranking", validPlayers, onlyValidPlayers, this::createLeaderboardEntry));
        if (!onlyValidPlayers) {
            embeds.addAll(this.createBoard("Unranked Players", unrankedPlayers, true, this::createUnrankedPlayerEntry));
        }
        return embeds;
    }

    private MessageEmbed createLeaderBoardHeader() {
        final String title = "__**Overwatch Leaderboard**__";
        final StringBuilder description = new StringBuilder();
        description.append("**Quick Help**").append("\n");
        description.append("Add your Battle tag: ").append("`!btag JeffKaplan#0815`").append("\n");
        description.append("Remove your Battle tag: ").append("`!remove JeffKaplan#0815`").append("\n");
        description.append("Remove all your tags: ").append("`!remove @YourDiscordName`").append("\n");
        return new Leaderboard(title, description.toString(), false);
    }

    private List<MessageEmbed> createBoard(String title, List<LeaderboardPlayer> players, boolean withFooter, Function<LeaderboardPlayer, String> leaderboardEntryCreator) {
        final List<MessageEmbed> embeds = new LinkedList<>();
        StringBuilder description = new StringBuilder();
        boolean firstEntry = true;
        for (final LeaderboardPlayer player : players) {
            final String entry = leaderboardEntryCreator.apply(player);
            if (description.length() + entry.length() + (firstEntry ? StringUtils.length(title) : 0) >= 2000) {
                if (firstEntry) {
                    embeds.add(new Leaderboard(title, description.toString()));
                    firstEntry = false;
                } else {
                    embeds.add(new Leaderboard(null, description.toString()));
                }
                description = new StringBuilder();
            }
            description.append(entry);
        }
        embeds.add(new Leaderboard(firstEntry ? title : null, description.toString(), withFooter));
        return embeds;
    }

    private String createUnrankedPlayerEntry(LeaderboardPlayer player) {
        final String btag = removePattern(player.getBattleTag(), "#\\d{4,6}");
        final String name = player.getUserAlias();
        final String teamRoles = player.getTeamRoleIds().stream().collect(Collectors.joining(""));
        return format("- %s*(%s)*   %s\n", btag, name, teamRoles);
    }

    private static class DefaultLeaderboardFactory extends LeaderboardFactory {
        @Override
        protected String createLeaderboardEntry(LeaderboardPlayer player) {
            final int place = player.getIndex();
            final int rating = player.getRating() > 0 ? player.getRating() : 0;
            final String btag = removePattern(player.getBattleTag(), "#\\d{4,6}");
            final String name = player.getUserAlias();
            return format("%02d. | %04d | %s*(%s)*\n", place, rating, btag, name);
        }

    }

    private static class ODCMLeaderboardFactory extends LeaderboardFactory {
        @Override
        protected String createLeaderboardEntry(LeaderboardPlayer player) {
            final int place = player.getIndex();
            final String rank = player.getRankId();
            final int rating = player.getRating() > 0 ? player.getRating() : 0;
            final String flag = player.getFlagId();
            final String btag = removePattern(player.getBattleTag(), "#\\d{4,6}");
            final String name = player.getUserAlias();
            final String teamRoles = player.getTeamRoleIds().stream().findFirst().orElse(":question:");
            return format("%02d. | %s | %04d | %s | %s | %s*(%s)*\n", place, rank, rating, flag, teamRoles, btag, name);
        }
    }

}
