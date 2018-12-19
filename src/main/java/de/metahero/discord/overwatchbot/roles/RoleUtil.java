package de.metahero.discord.overwatchbot.roles;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.core.entities.Role;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoleUtil<T> {

    @SuppressWarnings("unchecked")
    public static <B> RoleUtil<B> of(Class<B> clazz) {
        if (Flag.class.equals(clazz)) {
            return (RoleUtil<B>) new RoleUtil<>(Flag::isRole, Flag::fromRole);
        }
        if (Rank.class.equals(clazz)) {
            return (RoleUtil<B>) new RoleUtil<>(Rank::isRole, Rank::fromRole);
        }
        if (TeamRole.class.equals(clazz)) {
            return (RoleUtil<B>) new RoleUtil<>(TeamRole::isRole, TeamRole::fromRole);
        }
        throw new IllegalArgumentException("No RoleUtil for class:" + clazz);
    }

    private final Predicate<String> validator;
    private final Function<String, T> converter;

    public Optional<T> filterFirst(List<Role> roles) {
        final Optional<Role> filtered = this.filterFirstRaw(roles);
        return filtered.map(Role::getName).map(this.converter::apply);
    }

    public Optional<Role> filterFirstRaw(List<Role> roles) {
        for (final Role role : emptyIfNull(roles)) {
            if (this.validator.test(role.getName())) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    public List<T> filterAll(List<Role> roles) {
        final List<T> filtered = new ArrayList<>();
        for (final Role role : this.filterAllRaw(roles)) {
            final T temp = this.converter.apply(role.getName());
            filtered.add(temp);
        }
        return filtered;
    }

    public List<Role> filterAllRaw(List<Role> roles) {
        final List<Role> filtered = new ArrayList<>();
        for (final Role role : emptyIfNull(roles)) {
            if (this.validator.test(role.getName())) {
                filtered.add(role);
            }
        }
        return filtered;
    }
}
