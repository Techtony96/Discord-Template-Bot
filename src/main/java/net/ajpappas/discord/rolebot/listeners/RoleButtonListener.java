package net.ajpappas.discord.rolebot.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Role;
import discord4j.rest.http.client.ClientException;
import net.ajpappas.discord.common.exception.UserException;
import net.ajpappas.discord.common.util.ErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

@Component
public class RoleButtonListener {

    private static final String ADD_REASON = "Role added via interaction request";
    private static final String REMOVE_REASON = "Role removed via interaction request";


    @Autowired
    public RoleButtonListener(GatewayDiscordClient client) {
        client.on(ButtonInteractionEvent.class)
                .flatMap(event -> handle(event)
                        .onErrorResume(error -> ErrorHandler.handleError(error, msg -> event.reply(msg).withEphemeral(true))))
                .subscribe();
    }

    private Mono<Void> handle(ButtonInteractionEvent event) {
        if (!event.getCustomId().startsWith("role-assign:"))
            return Mono.empty();

        Snowflake roleId = Snowflake.of(event.getCustomId().split(":")[1]);
        Mono<String> roleNameMono = Mono.justOrEmpty(event.getInteraction().getGuildId()).flatMap(guildId -> event.getClient().getRoleById(guildId, roleId).map(Role::getMention));

        return Mono.justOrEmpty(event.getInteraction().getMember())
                .flatMap(member -> {
                    if (member.getRoleIds().contains(roleId)) {
                        return member.removeRole(roleId, REMOVE_REASON).thenReturn("removed");
                    } else {
                        return member.addRole(roleId, ADD_REASON).thenReturn("added");
                    }
                })
                .onErrorMap(ClientException.isStatusCode(403), error -> new UserException("Unable to update role, bot is missing permissions to manage roles"))
                .onErrorMap(error -> new UserException("Unexpected Exception: " + error.getMessage()))
                .zipWith(roleNameMono)
                .log()
                .flatMap(TupleUtils.function((action, roleName) -> event.reply(String.format("Successfully %s %s", action, roleName)).withEphemeral(true)));
    }
}
