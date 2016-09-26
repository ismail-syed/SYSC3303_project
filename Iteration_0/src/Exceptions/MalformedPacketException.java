package Exceptions;

@SuppressWarnings("serial")
public class MalformedPacketException extends Exception {
    public MalformedPacketException(String message) {
        super(message);
    }
}