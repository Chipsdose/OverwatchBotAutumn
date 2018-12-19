package de.metahero.discord.overwatchbot.util;

import java.util.Comparator;

import de.metahero.discord.overwatchbot.entity.Player;

public class PlayerComparator implements Comparator<Player> {

	@Override
	public int compare(Player o1, Player o2) {
		return Integer.compare(o1.getSeasonRating(), o2.getSeasonRating()) * -1;
	}

}
