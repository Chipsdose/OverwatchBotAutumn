package de.metahero.discord.overwatchbot.persistence;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.thoughtworks.xstream.XStream;

import de.metahero.discord.overwatchbot.entity.GuildList;
import de.metahero.discord.overwatchbot.entity.Player;
import de.metahero.discord.overwatchbot.entity.RegisteredGuild;
import de.metahero.discord.overwatchbot.entity.RegisteredTextChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XStreamHandler implements OverwatchBotDataHandler {
    private final static String FILE_SERVERS = "guilds.xml";

    private final XStream data;

    public XStreamHandler() {
        final Class<?>[] allowedTypes = { Player.class, RegisteredTextChannel.class, RegisteredGuild.class, GuildList.class };
        this.data = new XStream();
        XStream.setupDefaultSecurity(this.data);
        this.data.allowTypes(allowedTypes);
        this.data.processAnnotations(allowedTypes);
    }

    @Override
    public boolean save(final List<RegisteredGuild> servers) {
        try (FileOutputStream out = new FileOutputStream(FILE_SERVERS, false);) {
            final GuildList list = new GuildList(servers);
            this.data.toXML(list, out);
            return true;
        } catch (final Exception e) {
            log.error("Couldn't save channels", e);
        }
        return false;
    }

    @Override
    public List<RegisteredGuild> list() {
        try (final FileInputStream in = new FileInputStream(FILE_SERVERS);) {
            final GuildList list = (GuildList) this.data.fromXML(in);
            return new CopyOnWriteArrayList<>(list.getGuilds());
        } catch (final Exception e) {
            log.error("Couldn't read channels", e);
        }
        return new CopyOnWriteArrayList<>();
    }
}
