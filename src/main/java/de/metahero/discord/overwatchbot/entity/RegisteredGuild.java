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
import net.dv8tion.jda.core.entities.Guild;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@XStreamAlias("guild")
@ToString
public class RegisteredGuild {

    @Include
    @XStreamAsAttribute
    private long id;

    @NonNull
    @XStreamOmitField
    private Guild guild;

    @XStreamImplicit
    private CopyOnWriteArrayList<RegisteredTextChannel> channels;

    public List<RegisteredTextChannel> getChannels() {
        if (isNull(this.channels)) {
            this.channels = new CopyOnWriteArrayList<>();
        }
        return this.channels;
    }

}
