package sniper;

import java.math.BigInteger;
import org.apache.commons.lang3.tuple.Pair;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import sniper.config.RouterConfig;

public interface Router {
  String getAddress();
  Factory getFactory();
  RouterConfig.Type getType();
  Token getWeth();
  BigInteger getAmountOut(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn
  );
  BigInteger getAmountIn(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountOut
  );
  RemoteFunctionCall<TransactionReceipt> swapExactTokensForTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final BigInteger amountOutMin,
    final String to
  );
  RemoteFunctionCall<TransactionReceipt> swapExactTokensForTokensSupportingFeeOnTransferTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final BigInteger amountOutMin,
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
