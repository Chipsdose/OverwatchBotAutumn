package de.metahero.discord.overwatchbot.controller;

import static java.util.Objects.nonNull;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoleModifyQueueWorker implements ThreadFactory {
    private static final long QUEUE_DELAY_MILLIS = 2000;

    private static final AtomicInteger threadCounter = new AtomicInteger(1);

    private final Queue<Runnable> modificationQueue;

    private final ScheduledExecutorService executor;

    public RoleModifyQueueWorker() {
        this.modificationQueue = new ArrayBlockingQueue<>(10000, true);
        this.executor = Executors.newScheduledThreadPool(1, this);
        this.executor.scheduleWithFixedDelay(this::takeNextInQueueAndProcess, 0, QUEUE_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    public boolean queueRoleModification(Runnable roleModification) {
        return this.modificationQueue.offer(roleModification);
    }

    private void takeNextInQueueAndProcess() {
        final Runnable next = this.modificationQueue.poll();
        if (nonNull(next)) {
            try {
                next.run();
            } catch (final Exception e) {
                log.error("Unexpected Error on QueueWorker", e);
            }
        }
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, "RoleQueue-Worker-" + threadCounter.getAndIncrement());
    }

}
