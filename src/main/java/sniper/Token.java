package sniper;

import java.math.BigInteger;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface Token {
  String getAddress();
  BigInteger allowance(final String spender);
  RemoteFunctionCall<TransactionReceipt> approve(
    final String spender,
    final BigInteger amount
  );
  BigInteger getDecimals();
  String getSymbol();

  BigInteger getMaxTxAmount();
  String getUniswapV2Pair();
  BigInteger getLiquidityFee();
  BigInteger getMarketingFee();
  BigInteger getBurnFee();
  BigInteger getPotFee();
}
