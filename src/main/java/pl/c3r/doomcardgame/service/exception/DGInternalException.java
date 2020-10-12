package pl.c3r.doomcardgame.service.exception;

import java.text.MessageFormat;

public class DGInternalException extends RuntimeException {
    public DGInternalException(String message) {
        super(message);
    }

    public DGInternalException(String message, Object... args) {
        super(MessageFormat.format(message, args));
    }
}
