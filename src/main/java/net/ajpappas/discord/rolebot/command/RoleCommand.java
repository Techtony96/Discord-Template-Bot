package net.ajpappas.discord.rolebot.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import net.ajpappas.discord.common.command.SlashCommand;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class RoleCommand implements SlashCommand {

    private static final int BUTTONS_PER_ROW = 5; // Discord limits ActionRows to 5 buttons
    private static final String ARG_DELIMITER = "\\|";

    @Override
    public String getName() {
        return "rolemessage";
    }

    @Override
    public PermissionSet requiredPermissions() {
        return PermissionSet.of(Permission.MANAGE_ROLES);
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        List<Button> buttons = event.getOptions().stream()
                .filter(option -> option.getValue().isPresent())
                .collect(Collectors.groupingBy(option -> option.getName().replaceAll("[^0-9]", "")))
                .values().stream()
                .map(grouped -> {
                    ApplicationCommandInteractionOption role = grouped.stream().filter(option -> option.getName().startsWith("role")).findAny().get();
                    ApplicationCommandInteractionOption button = grouped.stream().filter(option -> option.getName().startsWith("button")).findAny().get();

                    String id = "role-assign:" + role.getValue().get().asRole().block().getId().asString();
                    String[] arg = button.getValue().map(ApplicationCommandInteractionOptionValue::asString).map(s -> s.split(ARG_DELIMITER, 2)).get();
                    String emoji = null, label = null;

                    switch (arg.length) {
                        case 2 -> {
                            emoji = arg[0];
                            label = arg[1];
                        }
                        case 1 -> label = arg[0];
                    }

                    return Button.primary(id, parseEmoji(emoji), label);

                }).collect(Collectors.toList());



        List<LayoutComponent> rows = IntStream.iterate(0, i -> i < buttons.size(), i -> i + BUTTONS_PER_ROW)
                .mapToObj(i -> buttons.subList(i, Math.min(i + BUTTONS_PER_ROW, buttons.size())))
                .map(ActionRow::of)
                .map(LayoutComponent.class::cast)
                .toList();

        MessageCreateSpec spec = MessageCreateSpec.builder()
                .addAllComponents(rows)
                .addEmbed(EmbedCreateSpec.builder()
                        .title("Roles")
                        .description("Click the below buttons to be assigned the given role. Be notified when others @TheGame looking for players or join private channels to discuss the given topic.")
                        .color(Color.TAHITI_GOLD)
                        .build())
                .build();

        return event.getInteraction().getChannel().flatMap(c -> c.createMessage(spec)).then(event.reply("Message Created").withEphemeral(true));
    }



    private static ReactionEmoji parseEmoji(String input) {
            if (input == null || input.trim().length() == 0)
                return null;
            return ReactionEmoji.unicode(input.trim());
    }
}


