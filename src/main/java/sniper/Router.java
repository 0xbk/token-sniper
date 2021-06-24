package sniper;

import java.math.BigInteger;
import java.util.List;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface Router {
  String getContractAddress();
  Factory factory();
  Token weth();
  List<BigInteger> getAmountsOut(final BigInteger amountIn, List<Token> path)
    throws Exception;

  public List<BigInteger> getAmountsIn(
    final BigInteger amountOut,
    List<Token> path
  )
    throws Exception;

  RemoteFunctionCall<TransactionReceipt> swapExactTokensForTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final String to
  )
    throws Exception;
  RemoteFunctionCall<TransactionReceipt> swapTokensForExactTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountOut,
    final String to
  )
    throws Exception;
}
