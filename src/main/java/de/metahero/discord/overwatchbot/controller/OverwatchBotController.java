package de.metahero.discord.overwatchbot.controller;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections4.ListUtils;

import de.metahero.discord.overwatchbot.entity.Player;
import de.metahero.discord.overwatchbot.entity.RegisteredGuild;
import de.metahero.discord.overwatchbot.entity.RegisteredTextChannel;
import de.metahero.discord.overwatchbot.leaderboard.LeaderboardConverter;
import de.metahero.discord.overwatchbot.leaderboard.LeaderboardFactory;
import de.metahero.discord.overwatchbot.leaderboard.LeaderboardPlayer;
import de.metahero.discord.overwatchbot.persistence.OverwatchBotDataHandler;
import de.metahero.discord.overwatchbot.roles.Rank;
import de.metahero.discord.overwatchbot.roles.RoleUtil;
import de.metahero.discord.overwatchbot.util.BattleTagSrReader;
import de.metahero.discord.overwatchbot.util.PlayerComparator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ResumedEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.requests.ErrorResponse;

@Slf4j
public class OverwatchBotController extends ListenerAdapter {
    private static final int MESSAGE_DELAY_SECONDS = 5;
    private final OverwatchBotDataHandler dataHandler;
    private final ScheduledExecutorService executor;
    private final List<RegisteredGuild> guilds;
    private final RoleModifyQueueWorker roleWorker;

    @Getter
    private JDA bot;

    public OverwatchBotController(final OverwatchBotDataHandler dataHandler) {
        super();
        log.debug("Initializing Bot Controller");
        this.executor = new ScheduledThreadPoolExecutor(4, OverwatchBotThreadHandler.getFactory("OBC-Thread"));
        this.dataHandler = dataHandler;
        this.guilds = dataHandler.list();
        this.roleWorker = new RoleModifyQueueWorker();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> dataHandler.save(this.guilds), "DataSave-Shutdown-Thread"));
    }

    public void start(String secret) throws LoginException, InterruptedException {
        if (isNull(this.bot)) {
            log.debug("Building bot");
            this.bot = this.createBotBuilder(secret).build();
        }
    }

    public void shutdown() throws InterruptedException {
        if (this.executor.awaitTermination(2, TimeUnit.SECONDS)) {
            this.executor.shutdownNow();
        }
        if (nonNull(this.bot)) {
            this.bot.shutdown();
        }
    }

    private JDABuilder createBotBuilder(String secret) {
        final JDABuilder botBuilder = new JDABuilder(AccountType.BOT);
        botBuilder.setAudioEnabled(false);
        // botBuilder.setBulkDeleteSplittingEnabled(false);
        botBuilder.setCorePoolSize(4);
        botBuilder.setGame(Game.watching("your SR"));
        botBuilder.setToken(secret);
        botBuilder.addEventListener(this);
        botBuilder.setAutoReconnect(true);
        return botBuilder;
    }

    private User getBotUser() {
        return this.getBot().getSelfUser();
    }

    @Override
    public void onReady(final ReadyEvent event) {
        super.onReady(event);
        log.debug("Bot ready");
        log.debug("Initialiying scheduled events");
        this.executor.scheduleAtFixedRate(this::rebuildAllTextChannels, 60, 60, TimeUnit.MINUTES);
        this.executor.scheduleAtFixedRate(this::updatePlayerSeasonRatings, 12, 12, TimeUnit.HOURS);
        this.executor.scheduleAtFixedRate(this::save, 1, 1, TimeUnit.HOURS);
        log.debug("Initial cleanUp");
        final CompletableFuture<?> taskSync = runAsync(this::synchronizeGuilds, this.executor);
        final CompletableFuture<?> taskStart = taskSync
                .whenCompleteAsync((o, t) -> this.handleAndRun(() -> this.streamRegisterdTextChannels().peek(this::cleanTextChannel).forEach(c -> c.getChannel().sendMessage(":gear: Starting Bot :gear:").queue()), t), this.executor);
        final CompletableFuture<?> taskUpdateSR = taskStart.whenCompleteAsync((o, t) -> this.handleAndRun(this::updatePlayerSeasonRatings, t), this.executor);
        final CompletableFuture<?> taskClearRebuild = taskUpdateSR.whenCompleteAsync((o, t) -> this.handleAndRun(this::rebuildAllTextChannels, t), this.executor);
        taskClearRebuild.whenCompleteAsync((o, t) -> this.handleAndRun(() -> log.debug("Initial cleanUp finished"), t), this.executor);
    }

    private void handleAndRun(Runnable runnable, Throwable t) {
        if (nonNull(t)) {
            log.error("Error on execution", t);
        }
        runnable.run();
    }

    @Override
    public void onResume(ResumedEvent event) {
        super.onResume(event);
        log.debug("Bot resume");
    }

    @Override
    public void onShutdown(final ShutdownEvent event) {
        super.onShutdown(event);
        log.info("Saving guilds on shutdown");
        this.dataHandler.save(this.guilds);
    }

    @Override
    public void onException(final ExceptionEvent event) {
        super.onException(event);
        log.error("Exception caught on bot", event.getCause());
    }

    @Override
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
        super.onGuildMessageReceived(event);
        if (this.getBotUser().equals(event.getAuthor())) {
            // Ignore bot message
        } else {
            if (this.isChannelRegistered(event.getChannel())) {
                this.handleRegisteredChannelEvent(event);
            } else {
                this.handleUnregisteredChannelEvent(event);
            }
        }
    }

    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        super.onGuildJoin(event);
        runAsync(this::synchronizeGuilds, this.executor);
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event) {
        super.onGuildLeave(event);
        runAsync(this::synchronizeGuilds, this.executor);
    }

    @Override
    public void onGuildBan(final GuildBanEvent event) {
        super.onGuildBan(event);
        runAsync(this::synchronizeGuilds, this.executor);
    }

    private void handleRegisteredChannelEvent(final GuildMessageReceivedEvent event) {
        log.debug("Handle event in registered channel[{}]", event.getChannel());
        final String message = event.getMessage().getContentRaw();
        final Optional<BotCommand> possibleCommand = BotCommand.ofValue(message);
        log.info("{}", possibleCommand);
        if (possibleCommand.isPresent()) {
            final BotCommand command = possibleCommand.get();
            if (command == BotCommand.BATTLETAG) {
                runAsync(() -> this.addNewPlayer(event, message), this.executor);
            } else if (command == BotCommand.REMOVE) {
                runAsync(() -> this.removePlayer(event, message), this.executor);
            } else if (command == BotCommand.UPDATE) {
                runAsync(() -> this.updateChannel(event), this.executor);
            } else if (command == BotCommand.UNREGISTER) {
                runAsync(() -> this.unregisterChannel(event), this.executor);
            } else {
                event.getChannel().sendMessage("Command not supported.").queue(this::deleteMessage);
            }
        }
        this.deleteMessage(event.getMessage());
    }

    private void addNewPlayer(final GuildMessageReceivedEvent event, String message) {
        final List<String> params = BotCommand.getParams(message);
        if (params.size() < 1) {
            event.getChannel().sendMessage("No BattleTag found").queue(this::deleteMessage);
            return;
        }
        final RegisteredTextChannel channel = this.getRegisteredFromChannel(event.getChannel()).get();
        final String battleTag = params.get(0);
        final long playerId = event.getAuthor().getIdLong();
        if (channel.getPlayers().stream().anyMatch(p -> p.getBattleTag().equals(battleTag))) {
            event.getChannel().sendMessage("BattleTag already registered").queue(this::deleteMessage);
            return;
        }
        final int sr = BattleTagSrReader.getSeasonRatingFor(battleTag);
        if (sr < 0) {
            event.getChannel().sendMessage("Something went wrong while checking your BattleTag. Either you are not placed yet, your BattleTag is wrong or an internal error occured.").queue(this::deleteMessage);
            return;
        }
        final Player player = new Player(playerId, battleTag, sr);
        channel.getPlayers().add(player);
        this.save();
        this.rebuildTextChannel(channel);
    }

    private void removePlayer(final GuildMessageReceivedEvent event, String message) {
        try {
            final Guild guild = event.getGuild();
            final User user = event.getAuthor();
            final boolean isAdmin = this.hasAdminPermission(guild, user);
            final RegisteredTextChannel channel = this.getRegisteredFromChannel(event.getChannel()).get();
            final List<String> params = BotCommand.getParams(message);
            if (params.size() <= 0) {
                channel.getPlayers().removeIf(p -> p.getId() == user.getIdLong());
                this.rebuildTextChannel(channel);
            } else {
                final List<Player> matchingPlayers = new ArrayList<>();
                for (final String param : params) {
                    if (param.matches(".*#\\d{4,6}")) {
                        matchingPlayers.addAll(ListUtils.subtract(channel.getPlayers().stream().filter(p -> p.getBattleTag().equals(param)).collect(Collectors.toList()), matchingPlayers));
                    }
                }
                for (final Member member : event.getMessage().getMentionedMembers()) {
                    matchingPlayers.addAll(ListUtils.subtract(channel.getPlayers().stream().filter(p -> p.getId() == member.getUser().getIdLong()).collect(Collectors.toList()), matchingPlayers));
                }
                for (final Player player : matchingPlayers) {
                    if (player.getId() == user.getIdLong() || isAdmin) {
                        channel.getPlayers().remove(player);
                    } else {
                        channel.getChannel().sendMessage("You have no rights to delete this entry").queue(this::deleteMessage);
                    }
                }
                if (matchingPlayers.size() > 0) {
                    this.save();
                    this.rebuildTextChannel(channel);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to remove player", e);
        }
    }

    private void unregisterChannel(final GuildMessageReceivedEvent event) {
        final Optional<RegisteredTextChannel> registeredTextChannel = this.getRegisteredFromChannel(event.getChannel());
        if (registeredTextChannel.isPresent()) {
            this.guilds.stream().filter(g -> g.getGuild().equals(event.getGuild())).forEach(g -> g.getChannels().remove(registeredTextChannel.get()));
            this.cleanTextChannel(registeredTextChannel.get());
        }
    }

    private void updateChannel(final GuildMessageReceivedEvent event) {
        if (this.hasAdminPermission(event.getGuild(), event.getAuthor())) {
            final RegisteredTextChannel channel = this.getRegisteredFromChannel(event.getChannel()).get();
            this.rebuildTextChannel(channel);
        }
    }

    private void handleUnregisteredChannelEvent(final GuildMessageReceivedEvent event) {
        log.trace("Handle event in unregistered channel[{}]", event.getChannel());
        final String message = event.getMessage().getContentRaw();
        final Optional<BotCommand> possibleCommand = BotCommand.ofValue(message);
        if (possibleCommand.isPresent() && possibleCommand.get() == BotCommand.REGISTER && this.hasAdminPermission(event.getGuild(), event.getAuthor())) {
            runAsync(() -> this.registerNewTextChannel(event), this.executor);
        } else {
            // Ignore no (register) command message in unregistered channel
        }
    }

    private void registerNewTextChannel(final GuildMessageReceivedEvent event) {
        final RegisteredTextChannel registeredTextChannel = new RegisteredTextChannel(event.getChannel().getIdLong(), event.getChannel(), null);
        log.info("Register: {}", this.channelToString(registeredTextChannel));
        RegisteredGuild registeredGuild = this.guilds.stream().filter(g -> g.getId() == event.getGuild().getIdLong()).findFirst().orElse(null);
        if (isNull(registeredGuild)) {
            registeredGuild = new RegisteredGuild(event.getGuild().getIdLong(), event.getGuild(), null);
            this.guilds.add(registeredGuild);
        }
        registeredGuild.getChannels().add(registeredTextChannel);
        this.rebuildTextChannel(registeredTextChannel);
    }

    private boolean isChannelRegistered(final TextChannel channel) {
        return this.streamTextChannels().anyMatch(channel::equals);
    }

    private Stream<RegisteredTextChannel> streamRegisterdTextChannels() {
        return this.guilds.stream().flatMap(guild -> guild.getChannels().stream());
    }

    private Stream<TextChannel> streamTextChannels() {
        return this.streamRegisterdTextChannels().map(RegisteredTextChannel::getChannel);
    }

    private Stream<Player> streamPlayers() {
        return this.streamRegisterdTextChannels().flatMap(channel -> channel.getPlayers().stream());
    }

    private boolean hasAdminPermission(Guild guild, User user) {
        return guild.getMember(user).hasPermission(Permission.ADMINISTRATOR) || user.getId().equalsIgnoreCase("154918139988148224") || guild.getMember(user).isOwner();
    }

    private Optional<RegisteredTextChannel> getRegisteredFromChannel(TextChannel channel) {
        return this.streamRegisterdTextChannels().filter(c -> c.getChannel().getId().equals(channel.getId())).findFirst();
    }

    private void synchronizeGuilds() {
        log.debug("Synchronizing guilds");
        try {
            final List<RegisteredGuild> currentGuilds = this.bot.getGuilds().stream().map(g -> new RegisteredGuild(g.getIdLong(), g, null)).collect(Collectors.toList());
            final List<RegisteredGuild> obsoleteGuilds = ListUtils.subtract(this.guilds, currentGuilds);
            obsoleteGuilds.forEach(g -> log.info("Guild[{}] not found. Deleting..", g.getId()));
            this.guilds.removeAll(obsoleteGuilds);

            final List<RegisteredGuild> newGuilds = ListUtils.removeAll(currentGuilds, this.guilds);
            newGuilds.stream().peek(g -> log.info("Adding new Guild[{}:{}]", g.getGuild().getName(), g.getId())).forEach(this.guilds::add);

            this.guilds.stream().forEach(this::synchronizeGuild);
            this.streamRegisterdTextChannels().forEach(c -> log.info("Registerd: {}", this.channelToString(c)));
        } catch (final Exception e) {
            log.error("Error on synchronized", e);
        }

    }

    private void synchronizeGuild(final RegisteredGuild registeredGuild) {
        final Guild guild = this.bot.getGuildById(registeredGuild.getId());
        if (nonNull(guild)) {
            registeredGuild.setGuild(guild);
            for (final RegisteredTextChannel registeredTextChannel : registeredGuild.getChannels()) {
                final TextChannel channel = this.bot.getTextChannelById(registeredTextChannel.getId());
                if (nonNull(channel)) {
                    registeredTextChannel.setChannel(channel);
                } else {
                    log.info("No Channel[{}] found. Deleting..", registeredTextChannel.getId());
                    registeredGuild.getChannels().remove(registeredTextChannel);
                }
            }
        } else {
            log.info("No Guild[{}] found. Deleting..", registeredGuild.getId());
            this.guilds.remove(registeredGuild);
        }
    }

    private void rebuildAllTextChannels() {
        log.info("Rebuilding all channels");
        this.streamRegisterdTextChannels().forEach(c -> runAsync(() -> this.rebuildTextChannel(c), this.executor));
    }

    private void rebuildTextChannel(RegisteredTextChannel registeredTextChannel) {
        this.cleanTextChannel(registeredTextChannel);
        this.buildTextChannel(registeredTextChannel);
    }

    private void buildTextChannel(final RegisteredTextChannel registeredTextChannel) {
        this.cleanTextChannel(registeredTextChannel);
        log.debug("Rebuild: {}", this.channelToString(registeredTextChannel));
        try {
            final Message updateMessage = registeredTextChannel.getChannel().sendMessage(":satellite: Updating Leaderboard :satellite:").complete();
            final TextChannel channel = registeredTextChannel.getChannel();
            final GuildController guildController = new GuildController(channel.getGuild());
            final LeaderboardConverter converter = new LeaderboardConverter(channel);
            log.debug("Prepare players: {}", this.channelToString(registeredTextChannel));
            final List<LeaderboardPlayer> players = registeredTextChannel.getPlayers().stream().sorted(new PlayerComparator()).map(converter).filter(Objects::nonNull).collect(Collectors.toList());
            log.debug("Correct player roles: {}", this.channelToString(registeredTextChannel));
            this.distinctPlayerToHisHighest(players).forEach(player -> this.correctRankRole(player, guildController));
            log.debug("Create leaderboard parts: {}", this.channelToString(registeredTextChannel));
            final LeaderboardFactory leaderboard = LeaderboardFactory.newInstance(channel.getGuild().getId().equals("466697825711816714") ? LeaderboardFactory.ODCM : LeaderboardFactory.DEFAULT);
            final List<MessageEmbed> embeds = leaderboard.createLeaderboard(players);
            log.debug("Sending [{}]-Parts to leaderboard: {}", embeds.size(), this.channelToString(registeredTextChannel));
            int i = 0;
            for (; i < embeds.size(); i++) {
                final int index = i;
                this.executor.schedule(() -> registeredTextChannel.getChannel().sendMessage(embeds.get(index)).queue(), MESSAGE_DELAY_SECONDS * i, TimeUnit.SECONDS);
            }
            this.deleteMessage(updateMessage, i * MESSAGE_DELAY_SECONDS);
        } catch (final Exception e) {
            log.error("Error on rebuilding channel[{}]", registeredTextChannel.getId(), e);
        }

    }

    private void cleanTextChannel(final RegisteredTextChannel registeredTextChannel) {
        log.debug("Clear: {}", this.channelToString(registeredTextChannel));
        final TextChannel channel = registeredTextChannel.getChannel();
        final MessageHistory history = new MessageHistory(channel);
        List<Message> messages = null;
        do {
            messages = history.retrievePast(100).complete();
            if (messages.size() == 1) {
                messages.get(0).delete().complete();
            } else if (messages.size() > 1) {
                channel.deleteMessages(messages).complete();
            }
        } while (nonNull(messages) && messages.size() > 0);
    }

    private List<LeaderboardPlayer> distinctPlayerToHisHighest(List<LeaderboardPlayer> players) {
        final List<LeaderboardPlayer> distinctPlayers = new ArrayList<>();
        for (final LeaderboardPlayer player : players) {
            if (distinctPlayers.contains(player)) {
                final LeaderboardPlayer currentPlayer = distinctPlayers.get(distinctPlayers.indexOf(player));
                if (currentPlayer.getRating() < player.getRating()) {
                    distinctPlayers.remove(currentPlayer);
                    distinctPlayers.add(player);
                }
            } else {
                distinctPlayers.add(player);
            }
        }
        return distinctPlayers;
    }

    private void correctRankRole(LeaderboardPlayer player, GuildController controller) {
        final Guild guild = controller.getGuild();
        final Member member = guild.getMember(player.getUser());
        final List<Role> currentRoles = RoleUtil.of(Rank.class).filterAllRaw(member.getRoles());
        final List<Role> newRoles = guild.getRolesByName(player.getRank().getDiscordRole(), true);
        log.debug("Player[{}], Current Rank Roles[{}], New Rank Roles[{}]", player, currentRoles.stream().map(Role::getName).collect(joining(",")), newRoles.stream().map(Role::getName).collect(joining(",")));
        final List<Role> intersection = ListUtils.intersection(currentRoles, newRoles);
        if (!intersection.isEmpty()) {
            currentRoles.removeAll(intersection);
            newRoles.removeAll(intersection);
        }
        if (currentRoles.isEmpty() && newRoles.isEmpty()) {
        } else if (!currentRoles.isEmpty() && newRoles.isEmpty()) {
            this.roleWorker.queueRoleModification(controller.removeRolesFromMember(member, currentRoles)::complete);
        } else if (currentRoles.isEmpty() && !newRoles.isEmpty()) {
            this.roleWorker.queueRoleModification(controller.addRolesToMember(member, newRoles)::complete);
        } else {
            if (!currentRoles.isEmpty() && !newRoles.isEmpty()) {
                this.roleWorker.queueRoleModification(controller.modifyMemberRoles(member, currentRoles, newRoles)::complete);
            }
        }
    }

    private void updatePlayerSeasonRatings() {
        for (final RegisteredTextChannel channel : this.streamRegisterdTextChannels().collect(Collectors.toList())) {
            for (final Player player : new ArrayList<>(channel.getPlayers())) {
                final Optional<User> user = Optional.ofNullable(this.bot.getUserById(player.getId()));
                if (!user.isPresent() || !channel.getChannel().getGuild().isMember(user.get())) {
                    channel.getPlayers().remove(player);
                    log.info("Removing Player[{}] from channel{}", player, this.channelToString(channel));
                }
            }
        }
        // TODO More efficienct algorithm than this
        final Map<String, List<Player>> players = new HashMap<>();
        for (final Player player : this.streamPlayers().collect(Collectors.toList())) {
            if (players.containsKey(player.getBattleTag())) {
                players.get(player.getBattleTag()).add(player);
            } else {
                players.put(player.getBattleTag(), new ArrayList<>(Arrays.asList(player)));
            }
        }
        players.entrySet().parallelStream().forEach(this::updatePlayerSeasonRating);
    }

    private void updatePlayerSeasonRating(Entry<String, List<Player>> entry) {
        final String btag = entry.getKey();
        final List<Player> players = entry.getValue();
        final int rating = BattleTagSrReader.getSeasonRatingFor(btag);
        players.forEach(p -> p.setSeasonRating(rating));
    }

    private ScheduledFuture<?> deleteMessage(final Message message) {
        return this.deleteMessage(message, 5);
    }

    private ScheduledFuture<?> deleteMessage(final Message message, final int delayInSeconds) {
        log.info("Deleting Message [{}]", message);
        return this.executor.schedule(() -> {
            try {
                message.delete().queue();
            } catch (final ErrorResponseException e) {
                if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                    log.warn("Message[{}] alread deleted", message);
                } else {
                    log.error("Couldn't delete Message[{}]", message, e);
                }
            }
        }, delayInSeconds, TimeUnit.SECONDS);
    }

    private void save() {
        log.info("Save data to file");
        this.dataHandler.save(this.guilds);
    }

    private String channelToString(RegisteredTextChannel c) {
        final StringBuilder b = new StringBuilder();
        b.append(format("Server[%s:%s]", c.getChannel().getGuild().getName(), c.getChannel().getGuild().getId()));
        b.append(format(", Channel[%s:%s]", c.getChannel().getName(), c.getChannel().getId()));
        if (nonNull(c.getChannel().getParent())) {
            b.append(format(", Category[%s:%s]", c.getChannel().getParent().getName(), c.getChannel().getParent().getId()));
        }
        return b.toString();
    }

}
