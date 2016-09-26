package Exceptions;

@SuppressWarnings("serial")
public class InvalidBlockNumberException extends Exception {
    public InvalidBlockNumberException(String message) {
        super(message);
    }
}