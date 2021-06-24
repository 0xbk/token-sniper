package sniper;

public interface Factory {
  TokenPair getPair(final Token token0, final Token token1);
}
