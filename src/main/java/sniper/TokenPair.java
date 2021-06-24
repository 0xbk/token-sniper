package sniper;

import java.math.BigInteger;

public interface TokenPair {
  static TokenPair from(final Token token0, final Token token1) {
    return TokenPairFactory.from(token0, token1);
  }

  BigInteger getDecimals();
  String getSymbol();
  Token getToken0();
  Token getToken1();
  BigInteger getTotalSupply();
}
