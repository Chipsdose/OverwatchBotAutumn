package de.metahero.discord.overwatchbot.entity;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@XStreamAlias("player")
@ToString
public class Player {

    @Include
    @XStreamAsAttribute
    private long id;

    @Include
    @NonNull
    @XStreamAsAttribute
    private String battleTag;

    @XStreamOmitField
    private int seasonRating = -1;
}
