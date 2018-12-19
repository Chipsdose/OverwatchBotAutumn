package de.metahero.discord.overwatchbot.entity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import static java.util.Objects.isNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.dv8tion.jda.core.entities.TextChannel;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@XStreamAlias("channel")
@ToString
public class RegisteredTextChannel {

    @Include
    @XStreamAsAttribute
    private long id;

    @NonNull
    @XStreamOmitField
    private TextChannel channel;

    @XStreamImplicit
    private CopyOnWriteArrayList<Player> players;

    public List<Player> getPlayers() {
        if (isNull(this.players)) {
            this.players = new CopyOnWriteArrayList<>();
        }
        return this.players;
    }

}
