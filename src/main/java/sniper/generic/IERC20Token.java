package sniper.generic;

import java.math.BigInteger;
import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.IERC20;
import org.web3j.generated.contracts.PrintrToken;
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

  private final PrintrToken ierc20;
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
      PrintrToken.load(contractAddress, web3j, txManager, gasProvider);
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
    log.info("Approving {} to spend {} {}", spender, amount, getSymbol());

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
  public BigInteger getBalanceLimit() {
    try {
      return ierc20.balanceLimit().send();
    } catch (final Exception e) {
      throw new SniperException("Failed to get balance limit.", e);
    }
  }

  @Override
  public BigInteger getSellLimit() {
    try {
      return ierc20.sellLimit().send();
    } catch (final Exception e) {
      throw new SniperException("Failed to get sell limit.", e);
    }
  }

  @Override
  public boolean isTradingEnabled() {
    try {
      return ierc20.tradingEnabled().send();
    } catch (final Exception e) {
      throw new SniperException("Failed to get is trading enabled.", e);
    }
  }

  @Override
  public boolean isWhiteListTrading() {
    try {
      return ierc20.whiteListTrading().send();
    } catch (final Exception e) {
      throw new SniperException("Failed to get is white list trading.", e);
    }
  }

  @Override
  public BigInteger getSellLockTimeInSeconds() {
    try {
      return ierc20.getSellLockTimeInSeconds().send();
    } catch (final Exception e) {
      throw new SniperException("Failed to get sell lock time in seconds.", e);
    }
  }
}
