package de.metahero.discord.overwatchbot.entity;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@XStreamAlias("guilds")
@NoArgsConstructor
@AllArgsConstructor
public class GuildList {

    @XStreamImplicit
    private List<RegisteredGuild> guilds;

    public List<RegisteredGuild> getGuilds() {
        if (isNull(this.guilds)) {
            this.guilds = new ArrayList<>();
        }
        return this.guilds;
    }
}
