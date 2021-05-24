package pl.c3r.doomcardgame.util;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MessageLog
{
    private List<String> messages;
    private static final DateTimeFormatter pattern = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");

    public MessageLog()
    {
        this.messages = new ArrayList<>();
    }

    public void debug(Logger logger, String msg, Object... args)
    {
        logger.debug(msg, args);
        String message = MessageFormatter.arrayFormat(msg, args).getMessage();
        String formattedTime = LocalDateTime.now().format(pattern);
        message = MessageFormat.format("{0}: {1}", formattedTime, message);
        messages.add(message);
    }

    public List<String> getMessages()
    {
        return messages;
    }
}
