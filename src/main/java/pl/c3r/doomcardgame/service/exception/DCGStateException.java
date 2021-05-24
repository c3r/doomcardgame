package pl.c3r.doomcardgame.service.exception;

import java.text.MessageFormat;

public class DCGStateException extends RuntimeException
{
    public DCGStateException(String message, Object... args)
    {
        super(MessageFormat.format(message, args));
    }

    public DCGStateException(String message)
    {
        super(message);
    }
}
