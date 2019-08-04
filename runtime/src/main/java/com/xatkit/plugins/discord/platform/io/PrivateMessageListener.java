package com.xatkit.plugins.discord.platform.io;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.plugins.chat.ChatUtils;
import com.xatkit.plugins.discord.DiscordUtils;
import fr.inria.atlanmod.commons.log.Log;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link ListenerAdapter} that listen private messages and send them to the underlying {@link XatkitCore}.
 * <p>
 * This class is instantiated by its containing {@link DiscordIntentProvider}, and is used to react to Discord
 * messages. Note that this listener only forwards not empty, not {@code null} messages to the {@link XatkitCore}, in
 * order to avoid useless calls to the underlying DialogFlow API.
 */
public class PrivateMessageListener extends ListenerAdapter {

    /**
     * The {@link XatkitCore} instance used to handle received messages.
     */
    private XatkitCore xatkitCore;

    /**
     * The {@link DiscordIntentProvider} managing this listener.
     */
    private DiscordIntentProvider discordIntentProvider;

    /**
     * Constructs a new {@link PrivateMessageListener} from the provided {@code xatkitCore} and {@code
     * discordIntentProvider}.
     *
     * @param xatkitCore            the {@link XatkitCore} instance used to handled received messages
     * @param discordIntentProvider the {@link DiscordIntentProvider} managing this listener
     * @throws NullPointerException if the provided {@code xatkitCore} is {@code null}
     */
    public PrivateMessageListener(XatkitCore xatkitCore, DiscordIntentProvider discordIntentProvider) {
        super();
        checkNotNull(xatkitCore, "Cannot construct a %s from a null %s", this.getClass().getSimpleName(),
                XatkitCore.class.getSimpleName());
        checkNotNull(discordIntentProvider, "Cannot construct a %s from a null %s", this.getClass().getSimpleName(),
                DiscordIntentProvider.class.getSimpleName());
        this.xatkitCore = xatkitCore;
        this.discordIntentProvider = discordIntentProvider;
    }

    /**
     * Returns the {@link XatkitCore} instance used to handled received messages.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the {@link XatkitCore} instance used to handle received messages
     */
    protected XatkitCore getXatkitCore() {
        return this.xatkitCore;
    }

    /**
     * Handles the received private message event and forward it to the {@link XatkitCore}.
     * <p>
     * Empty and {@code null} messages are filtered in order to avoid useless calls to the underlying DialogFlow API.
     * In addition, messages send by other bots are also ignored to avoid infinite discussion loops.
     *
     * @param event the {@link PrivateMessageReceivedEvent} wrapping the received message
     * @throws NullPointerException if the provided event is {@code null}
     */
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        checkNotNull(event, "Cannot handle a null %s", PrivateMessageReceivedEvent.class.getSimpleName());
        User author = event.getAuthor();
        checkNotNull(author, "Cannot handle a message from a null author");
        if (author.isBot()) {
            return;
        }
        MessageChannel channel = event.getChannel();
        Message message = event.getMessage();
        String content = message.getContentRaw();
        if (content.isEmpty()) {
            Log.trace("Skipping {0}, the message is empty");
            return;
        }
        Log.info("Received message {0} from user {1} (id: {2}, channel: {3})", content, author.getName(),
                author.getId(), channel.getId());
        XatkitSession xatkitSession = discordIntentProvider.getRuntimePlatform().createSessionFromChannel(channel);
        /*
         * Call getRecognizedIntent before setting any context variable, the recognition triggers a decrement of all
         * the context variables.
         */
        RecognizedIntent recognizedIntent = discordIntentProvider.getRecognizedIntent(content, xatkitSession);
        /*
         * The discord-related values are stored in the local context with a lifespan count of 1: they are reset
         * every time a message is received, and may cause consistency issues when using multiple IntentProviders.
         */
        xatkitSession.getRuntimeContexts().setContextValue(DiscordUtils.DISCORD_CONTEXT_KEY, 1, DiscordUtils
                .CHAT_CHANNEL_CONTEXT_KEY, channel.getId());
        xatkitSession.getRuntimeContexts().setContextValue(DiscordUtils.DISCORD_CONTEXT_KEY, 1, DiscordUtils
                .CHAT_USERNAME_CONTEXT_KEY, author.getName());
        xatkitSession.getRuntimeContexts().setContextValue(DiscordUtils.DISCORD_CONTEXT_KEY, 1,
                DiscordUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY, content);
        /*
         * Copy the variables in the chat context (this context is inherited from the
         * Chat platform)
         */
        xatkitSession.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                ChatUtils.CHAT_CHANNEL_CONTEXT_KEY, channel.getId());
        xatkitSession.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                ChatUtils.CHAT_USERNAME_CONTEXT_KEY, author.getName());
        xatkitSession.getRuntimeContexts().setContextValue(ChatUtils.CHAT_CONTEXT_KEY, 1,
                ChatUtils.CHAT_RAW_MESSAGE_CONTEXT_KEY, content);
        this.discordIntentProvider.sendEventInstance(recognizedIntent, xatkitSession);
    }
}
