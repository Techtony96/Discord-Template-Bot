package net.ajpappas.discord.rolebot.service;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RoleMessageManager {

    private Map<Snowflake, Message> roleMessage;

    @Autowired
    public RoleMessageManager(GatewayDiscordClient client) {

    }


}
