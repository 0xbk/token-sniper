package sniper.generic;

import java.math.BigInteger;
import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.SomeToken;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import sniper.GasStation;
import sniper.SniperException;
import sniper.Token;

@Log4j2
public class IERC20Token implements Token {

  private final SomeToken ierc20;
  private final TransactionManager txManager;
  private final BigInteger decimals;
  private final String symbol;

  public IERC20Token(
    final String contractAddress,
    final Web3j web3j,
    final TransactionManager txManager,
    final GasStation gasStation,
    final ContractGasProvider gasProvider
  ) {
    log.traceEntry(
      () -> contractAddress,
      () -> web3j,
      () -> txManager,
      () -> gasStation,
      () -> gasProvider
    );

    this.ierc20 =
      SomeToken.load(contractAddress, web3j, txManager, gasProvider);
    this.txManager = txManager;

    try {
      this.decimals = this.ierc20.decimals().send();
      this.symbol = this.ierc20.symbol().send();
    } catch (final Exception e) {
      throw new SniperException("Failed to create a token.", e);
    }

    log.info("Created {} token @ address {}", getSymbol(), getAddress());

    log.traceExit();
  }

  @Override
  public String getAddress() {
    log.traceEntry();
    return log.traceExit(ierc20.getContractAddress());
  }

  @Override
  public BigInteger allowance(String spender) {
    log.traceEntry();

    try {
      return log.traceExit(
        ierc20.allowance(txManager.getFromAddress(), spender).send()
      );
    } catch (final Exception e) {
      throw new SniperException("Failed to get allowance.", e);
    }
  }

  @Override
  public RemoteFunctionCall<TransactionReceipt> approve(
    final String spender,
    final BigInteger amount
  ) {
    log.traceEntry(() -> spender, () -> amount);
    return log.traceExit(ierc20.approve(spender, amount));
  }

  @Override
  public BigInteger getDecimals() {
    log.traceEntry();
    return log.traceExit(decimals);
  }

  @Override
  public String getSymbol() {
    log.traceEntry();
    return log.traceExit(symbol);
  }

  @Override
  public BigInteger getMaxTxAmount() {
    try {
      return ierc20._maxTxAmount().send();
    } catch (final Exception e) {
      throw new SniperException("Failed to get max tx amount.", e);
    }
  }

  @Override
  public String getUniswapV2Pair() {
    try {
      return ierc20.uniswapV2Pair().send();
    } catch (final Exception e) {
      throw new SniperException("Failed to get uniswap v2 pair.", e);
    }
  }
}
