package de.metahero.discord.overwatchbot.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

public enum BotCommand {
    REGISTER("register"),

    UNREGISTER("unregister"),

    BATTLETAG("btag"),

    REMOVE("remove"),

    UPDATE("update")

    ;
    private static final String PREFIX = "!";

    @Getter
    private final String command;

    private BotCommand(final String command) {
        this.command = command;
    }

    public static Optional<BotCommand> ofValue(final String command) {
        if (isCommand(command)) {
            final String commandRaw = StringUtils.split(StringUtils.removeStart(command, PREFIX))[0];
            return Arrays.stream(BotCommand.values()).filter(bc -> bc.getCommand().equals(commandRaw)).findFirst();
        }
        return Optional.empty();
    }

    public static boolean isCommand(String command) {
        return command.startsWith(PREFIX) && command.trim().length() > 1 && StringUtils.split(command)[0].length() > 1;
    }

    public static boolean hasParams(String command) {
        return !getParams(command).isEmpty();
    }

    public static List<String> getParams(String command) {
        if (isCommand(command)) {
            final List<String> params = new ArrayList<>(Arrays.asList(StringUtils.split(command)));
            params.remove(0);
            return params;
        }
        return Collections.emptyList();
    }

}
