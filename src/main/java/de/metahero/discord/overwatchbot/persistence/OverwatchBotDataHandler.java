package de.metahero.discord.overwatchbot.persistence;

import java.util.List;

import de.metahero.discord.overwatchbot.entity.RegisteredGuild;

public interface OverwatchBotDataHandler {

    boolean save(final List<RegisteredGuild> server);

    List<RegisteredGuild> list();

}
