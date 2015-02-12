package com.boothj5.commandbot;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class BotListener implements PacketListener {
    private static final Logger LOG = LoggerFactory.getLogger(BotListener.class);

    public static final String LIST_COMMAND = ":list";

    private final PluginStore plugins;
    private final MultiUserChat muc;
    private final String myNick;

    public BotListener(PluginStore plugins, MultiUserChat muc, String myNick) {
        this.plugins = plugins;
        this.muc = muc;
        this.myNick = myNick;
    }

    @Override
    public void processPacket(Packet packet) {
        try {
            if (packet instanceof Message) {
                Message messageStanza = (Message) packet;
                String message = messageStanza.getBody();
                if (validCommand(messageStanza)) {
                    if (message.equals(LIST_COMMAND)) {
                        handleListCommand();
                    } else {
                        handlePluginCommand(messageStanza);
                    }
                }
            }
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    private void handlePluginCommand(Message message) throws XMPPException {
        String command = parseCommand(message.getBody());
        if (plugins.exists(command)) {
            LOG.debug(format("Handling command: %s", command));
            plugins.get(command).onMessage(muc, message.getFrom(), message.getBody());
        } else {
            LOG.debug(format("Plugin does not exist: %s", command));
            muc.sendMessage("No such command: " + command);
        }
    }

    private void handleListCommand() throws XMPPException {
        String help = plugins.getHelp();
        muc.sendMessage(help);
    }

    private String parseCommand(String message) {
        String[] tokens = StringUtils.split(message, " ");
        return tokens[0].substring(1);
    }

    private boolean validCommand(Message messageStanza) {
        boolean containsBody = messageStanza.getBody() != null;
        boolean delayed = messageStanza.toXML().contains("delay");
        boolean fromMe = messageStanza.getFrom().endsWith(myNick);

        if (containsBody && !delayed && !fromMe) {
            LOG.debug("Received: " + messageStanza.getBody());
        }

        boolean isCommand = messageStanza.getBody().startsWith(":");

        return containsBody && !delayed && !fromMe && isCommand;
    }
}
