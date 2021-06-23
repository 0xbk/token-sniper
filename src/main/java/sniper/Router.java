package sniper;

import java.math.BigInteger;

public interface Router {
  public void swapExactTokensForTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final String to
  )
    throws Exception;

  void swapTokensForExactTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountOut,
    final String to
  )
    throws Exception;
}
