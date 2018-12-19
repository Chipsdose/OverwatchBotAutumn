package de.metahero.discord.overwatchbot;

import java.time.ZoneId;
import java.util.TimeZone;

import de.metahero.discord.overwatchbot.controller.OverwatchBotController;
import de.metahero.discord.overwatchbot.persistence.OverwatchBotDataHandler;
import de.metahero.discord.overwatchbot.persistence.XStreamHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OverwatchBot {

    public static void main(final String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("CET")));
        new OverwatchBot().start(args);
    }

    private final OverwatchBotController bot;

    public OverwatchBot() {
        final OverwatchBotDataHandler dataHandler = new XStreamHandler();
        this.bot = new OverwatchBotController(dataHandler);
    }

    private void start(final String[] args) {
        try {
            log.info("Starting OverwatchBot");
            this.bot.start(args[0]);
        } catch (final Exception e) {
            log.error("Bot failed to start up", e);
        }

    }

}
