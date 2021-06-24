package sniper;

public interface Factory {
  TokenPair getPair(final String token0Address, final String token1Address)
    throws Exception;
}
