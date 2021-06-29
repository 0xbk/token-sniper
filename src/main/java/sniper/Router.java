package sniper;

import java.math.BigInteger;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import sniper.config.RouterConfig;

public interface Router {
  String getAddress();
  Factory getFactory();
  RouterConfig.Type getType();
  Token getWeth();
  BigInteger getAmountOut(final Token tokenIn, final Token tokenOut, final BigInteger amountIn);
  List<BigInteger> getAmountsIn(final BigInteger amountOut, List<Token> path);
  Pair<BigInteger, RemoteFunctionCall<TransactionReceipt>> swapExactTokensForTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final BigInteger amountOutMin,
    final String to
  );
  RemoteFunctionCall<TransactionReceipt> swapExactTokensForAnyTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final String to
  );
  Pair<BigInteger, RemoteFunctionCall<TransactionReceipt>> swapTokensForExactTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountInMax,
    final BigInteger amountOut,
    final String to
  );
}
