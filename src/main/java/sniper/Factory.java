package sniper;

public interface Factory {
  Pair getPair(final Token token0, final Token token1) throws Exception;
}
