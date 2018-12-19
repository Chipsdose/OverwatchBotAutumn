package de.metahero.discord.overwatchbot.leaderboard;

import java.util.List;

import de.metahero.discord.overwatchbot.roles.Flag;
import de.metahero.discord.overwatchbot.roles.Rank;
import de.metahero.discord.overwatchbot.roles.TeamRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.core.entities.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LeaderboardPlayer {

    private int index;

    @Include
    private User user;

    private String userAlias;

    private String battleTag;

    private int rating;

    private Flag flag;

    private String flagId;

    private Rank rank;

    private String rankId;

    private List<TeamRole> teamRoles;

    private List<String> teamRoleIds;

}
