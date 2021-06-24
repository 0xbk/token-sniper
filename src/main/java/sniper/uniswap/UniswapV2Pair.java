package sniper.uniswap;

import java.math.BigInteger;
import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.IUniswapV2Pair;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import sniper.EstimatingGasProvider;
import sniper.GasStation;
import sniper.Token;
import sniper.TokenPair;
import sniper.generic.IERC20Token;

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
    final GasStation gasStation
  )
    throws Exception {
    log.traceEntry(contractAddress, web3j, txManager);

    this.pair =
      IUniswapV2Pair.load(
        contractAddress,
        web3j,
        txManager,
        new EstimatingGasProvider(contractAddress, web3j, txManager, gasStation)
      );
    this.decimals = this.pair.decimals().send();
    this.symbol = this.pair.symbol().send();
    this.token0 =
      new IERC20Token(this.pair.token0().send(), web3j, txManager, gasStation);
    this.token1 =
      new IERC20Token(this.pair.token1().send(), web3j, txManager, gasStation);

    log.info(
      "Created pair {}-{} ({}) @ address {}",
      token0.symbol(),
      token1.symbol(),
      symbol(),
      contractAddress
    );
    log.traceExit();
  }

  public BigInteger decimals() {
    return decimals;
  }

  public String symbol() {
    log.traceEntry();
    return log.traceExit(symbol);
  }

  public Token token0() {
    return token0;
  }

  public Token token1() {
    return token1;
  }

  public BigInteger totalSupply() throws Exception {
    log.traceEntry();
    return log.traceExit(pair.totalSupply().send());
  }
}
