package Exceptions;

@SuppressWarnings("serial")
public class PacketOverflowException extends Exception {
    public PacketOverflowException(String message) {
        super(message);
    }
}
