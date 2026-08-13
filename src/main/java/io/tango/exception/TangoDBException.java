package io.tango.exception;

/**
 * Base runtime exception thrown for any unrecoverable storage engine or I/O failure in TangoDB.
 *
 * @author Gokul G
 * @version 0.1.1
 */
public class TangoDBException extends RuntimeException {

    /**
     * Constructs a new {@code TangoDBException} with the specified detail message and cause.
     *
     * @param message   descriptive error message
     * @param throwable underlying cause exception
     */
    public TangoDBException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a new {@code TangoDBException} with the specified detail message.
     *
     * @param message descriptive error message
     */
    public TangoDBException(String message) {
        super(message);
    }
}
