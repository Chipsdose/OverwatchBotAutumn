package de.metahero.discord.overwatchbot.leaderboard;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.dv8tion.jda.core.entities.EmbedType;
import net.dv8tion.jda.core.entities.MessageEmbed;

public class Leaderboard extends MessageEmbed {

    public Leaderboard(String title, String description) {
        this(title, description, false);
    }

    public Leaderboard(String title, String description, boolean footer) {
        this(null, title, description, null, null, 0, null, null, null, null, footer ? createFooter() : null, null, Collections.emptyList());
    }

    protected Leaderboard(String url, String title, String description, EmbedType type, OffsetDateTime timestamp, int color, Thumbnail thumbnail, Provider siteProvider, AuthorInfo author, VideoInfo videoInfo, Footer footer, ImageInfo image,
            List<Field> fields) {
        super(url, title, description, type, timestamp, color, thumbnail, siteProvider, author, videoInfo, footer, image, fields);
    }

    private static Footer createFooter() {
        return new Footer(String.format("Last updated at %s - Written by Metahero", new Date()), null, null);
    }

}
