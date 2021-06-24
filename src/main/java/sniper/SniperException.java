package sniper;

public class SniperException extends RuntimeException {

  public SniperException(final String message) {
    super(message);
  }

  public SniperException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
