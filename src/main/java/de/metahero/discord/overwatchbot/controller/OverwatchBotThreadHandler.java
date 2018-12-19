package de.metahero.discord.overwatchbot.controller;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OverwatchBotThreadHandler implements ThreadFactory, UncaughtExceptionHandler {

    public static ThreadFactory getFactory(String name) {
        return new OverwatchBotThreadHandler(name, new AtomicInteger(1));
    }

    private final String name;
    private final AtomicInteger threadCounter;

    @Override
    public Thread newThread(final Runnable r) {
        final Thread thread = new Thread(r, this.name + "-" + this.threadCounter.getAndIncrement());
        thread.setUncaughtExceptionHandler(this::uncaughtException);
        return thread;
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        log.error("Error on Thread[{}] caught.", t.getName(), e);
    }

}
