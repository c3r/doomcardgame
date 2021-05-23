package pl.c3r.doomcardgame.service.exception;

import java.text.MessageFormat;

public class DGStateException extends RuntimeException
{
    public DGStateException(String message, Object... args)
    {
        super(MessageFormat.format(message, args));
    }

    public DGStateException(String message)
    {
        super(message);
    }
}
