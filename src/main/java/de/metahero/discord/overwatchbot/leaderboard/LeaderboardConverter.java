package de.metahero.discord.overwatchbot.leaderboard;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import static java.util.Objects.isNull;

import de.metahero.discord.overwatchbot.entity.Player;
import de.metahero.discord.overwatchbot.roles.Flag;
import de.metahero.discord.overwatchbot.roles.Rank;
import de.metahero.discord.overwatchbot.roles.RoleUtil;
import de.metahero.discord.overwatchbot.roles.TeamRole;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

@Slf4j
public class LeaderboardConverter implements Function<Player, LeaderboardPlayer> {
    public static final String DEFAULT_EMOTE = ":question:";
    private final TextChannel channel;
    private final Guild guild;
    private final JDA jda;
    private final AtomicInteger index = new AtomicInteger(1);

    private final RoleUtil<Flag> flagUtil = RoleUtil.of(Flag.class);
    private final RoleUtil<TeamRole> teamRoleUtil = RoleUtil.of(TeamRole.class);

    public LeaderboardConverter(TextChannel channel) {
        super();
        this.channel = channel;
        this.guild = this.channel.getGuild();
        this.jda = this.guild.getJDA();
    }

    @Override
    public LeaderboardPlayer apply(Player player) {
        log.debug("Convertig player[{}]", player);
        try {
            if (isNull(player)) {
                throw new IllegalArgumentException("Player must not be [null]");
            }
            final User user = this.jda.getUserById(player.getId());
            if (user == null) {
                log.debug("No user found for player[{}]", player);
                return null;
            }
            final Member member = this.guild.getMember(user);
            if (member == null) {
                log.debug("No member found for player[{}] in guild[{}]", player, this.guild.getName());
                return null;
            }

            final String userName = member.getEffectiveName();
            final List<Role> userRoles = member.getRoles();

            final Rank rank = Rank.fromRating(player.getSeasonRating());
            final Flag flag = this.flagUtil.filterFirst(userRoles).orElse(Flag.UNKNOWN);
            final List<TeamRole> teamRoles = this.teamRoleUtil.filterAll(userRoles);

            final String rankEmote = this.clarifyDiscordEmote(rank.getDiscordEmote());
            final String flagEmote = this.clarifyDiscordEmote(flag.getDiscordEmote());
            final List<String> teamRoleEmotes = teamRoles.stream().map(TeamRole::getDiscordEmote).map(this::clarifyDiscordEmote).filter(e -> !e.equals(DEFAULT_EMOTE)).collect(toList());

            final LeaderboardPlayer leaderboardPlayer = new LeaderboardPlayer();
            leaderboardPlayer.setIndex(this.index.getAndIncrement());
            leaderboardPlayer.setBattleTag(player.getBattleTag());
            leaderboardPlayer.setUserAlias(userName);
            leaderboardPlayer.setUser(user);
            leaderboardPlayer.setRating(player.getSeasonRating());
            leaderboardPlayer.setRank(rank);
            leaderboardPlayer.setRankId(rankEmote);
            leaderboardPlayer.setFlag(flag);
            leaderboardPlayer.setFlagId(flagEmote);
            leaderboardPlayer.setTeamRoles(teamRoles);
            leaderboardPlayer.setTeamRoleIds(teamRoleEmotes);
            return leaderboardPlayer;
        } catch (final Exception e) {
            log.error("Error while converting player[{}]", player, e);
            return null;
        }
    }

    private String clarifyDiscordEmote(String rawEmoteInfo) {
        if (StringUtils.startsWith(rawEmoteInfo, ":") && StringUtils.endsWith(rawEmoteInfo, ":")) {
            return rawEmoteInfo;
        }
        return this.guild.getEmotesByName(rawEmoteInfo, true).stream().findFirst().map(Emote::getAsMention).orElse(":question:");
    }

}
