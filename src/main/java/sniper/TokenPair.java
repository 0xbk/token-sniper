package sniper;

import java.math.BigInteger;

public interface TokenPair {
  String symbol();
  BigInteger totalSupply() throws Exception;
  Token token0();
  Token token1();
}
