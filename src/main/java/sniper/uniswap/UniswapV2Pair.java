package sniper.uniswap;

import java.math.BigInteger;
import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.IUniswapV2Pair;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import sniper.GasStation;
import sniper.SniperException;
import sniper.Token;
import sniper.TokenFactory;
import sniper.TokenPair;

@Log4j2
public class UniswapV2Pair implements TokenPair {

  private final IUniswapV2Pair pair;
  private final BigInteger decimals;
  private final String symbol;
  private final Token token0;
  private final Token token1;

  public UniswapV2Pair(
    final String contractAddress,
    final Web3j web3j,
    final TransactionManager txManager,
    final GasStation gasStation,
    final ContractGasProvider gasProvider,
    final TokenFactory token
  ) {
    log.traceEntry(
      () -> contractAddress,
      () -> web3j,
      () -> txManager,
      () -> gasStation,
      () -> gasProvider
    );

    this.pair =
      IUniswapV2Pair.load(contractAddress, web3j, txManager, gasProvider);

    try {
      this.decimals = this.pair.decimals().send();
      this.symbol = this.pair.symbol().send();
      this.token0 = token.from(this.pair.token0().send());
      this.token1 = token.from(this.pair.token1().send());
    } catch (final Exception e) {
      throw new SniperException("Failed to create a pair.", e);
    }

    log.info(
      "Created pair {}-{} ({}) @ address {}",
      token0.getSymbol(),
      token1.getSymbol(),
      getSymbol(),
      contractAddress
    );
    log.traceExit();
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
  public Token getToken0() {
    log.traceEntry();
    return log.traceExit(token0);
  }

  @Override
  public Token getToken1() {
    log.traceEntry();
    return log.traceExit(token1);
  }

  @Override
  public BigInteger getTotalSupply() {
    log.traceEntry();

    try {
      return log.traceExit(pair.totalSupply().send());
    } catch (final Exception e) {
      throw new SniperException("Failed to get the total supply.", e);
    }
  }
}
