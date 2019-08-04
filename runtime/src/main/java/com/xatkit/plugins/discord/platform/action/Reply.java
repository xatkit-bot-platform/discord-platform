package com.xatkit.plugins.discord.platform.action;

import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.discord.DiscordUtils;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.plugins.discord.platform.DiscordPlatform;
import fr.inria.atlanmod.commons.log.Log;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A {@link RuntimeAction} that replies to a message using the input Discord channel.
 * <p>
 * This action relies on the provided {@link XatkitSession} to retrieve the Discord {@code channel} associated to the
 * user input.
 * <p>
 * This class relies on the {@link DiscordPlatform}'s {@link net.dv8tion.jda.core.JDA} client to connect to the Discord
 * API and post the reply message.
 * <p>
 * <b>Note:</b> this class requires that its containing {@link DiscordPlatform} has been loaded with a valid Discord
 * bot token in order to authenticate the bot and post messages.
 *
 * @see PostMessage
 */
public class Reply extends PostMessage {

    /**
     * Returns the channel associated to the user input.
     * <p>
     * This method searches in the provided {@link RuntimeContexts} for the values stored with the key
     * {@link DiscordUtils#DISCORD_CONTEXT_KEY}.{@link DiscordUtils#CHAT_CHANNEL_CONTEXT_KEY}. Note
     * that if the provided {@link RuntimeContexts} does not contain the requested value a {@link NullPointerException}
     * is thrown.
     *
     * @param context the {@link RuntimeContexts} to retrieve the Discord channel from
     * @return the Discord channel associated to the user input
     * @throws NullPointerException     if the provided {@code context} is {@code null}, or if it does not contain the
     *                                  channel information
     * @throws IllegalArgumentException if the retrieved channel is not a {@link String}
     * @see DiscordUtils
     */
    private static String getChannel(RuntimeContexts context) {
        Object channelValue = context.getContextValue(DiscordUtils.DISCORD_CONTEXT_KEY, DiscordUtils
                .CHAT_CHANNEL_CONTEXT_KEY);
        checkNotNull(channelValue, "Cannot retrieve the Discord channel from the context");
        checkArgument(channelValue instanceof String, "Invalid Discord channel type, expected %s, found %s", String
                .class.getSimpleName(), channelValue.getClass().getSimpleName());
        Log.info("Found channel {0}", channelValue);
        return (String) channelValue;
    }

    /**
     * Constructs a new {@link Reply} with the provided {@code runtimePlatform}, {@code session}, and {@code message}.
     *
     * @param runtimePlatform the {@link DiscordPlatform} containing this action
     * @param session          the {@link XatkitSession} associated to this action
     * @param message          the message to post
     * @throws NullPointerException     if the provided {@code runtimePlatform} or {@code session} is {@code null}
     * @throws IllegalArgumentException if the provided {@code message} is {@code null} or empty
     * @see PostMessage#PostMessage(DiscordPlatform, XatkitSession, String, String)
     */
    public Reply(DiscordPlatform runtimePlatform, XatkitSession session, String message) {
        super(runtimePlatform, session, message, getChannel(session.getRuntimeContexts()));
    }
}
