package pl.c3r.doomcardgame.service.exception;

import java.text.MessageFormat;

public class DCGInternalException extends RuntimeException
{
    public DCGInternalException(String message)
    {
        super(message);
    }

    public DCGInternalException(String message, Object... args)
    {
        super(MessageFormat.format(message, args));
    }
}
