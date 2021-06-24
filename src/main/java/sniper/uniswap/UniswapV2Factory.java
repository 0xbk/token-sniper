package sniper.uniswap;

import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.IUniswapV2Factory;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import sniper.EstimatingGasProvider;
import sniper.Factory;
import sniper.GasStation;
import sniper.TokenPair;

@Log4j2
public class UniswapV2Factory implements Factory {

  private final IUniswapV2Factory factory;
  private final Web3j web3j;
  private final TransactionManager txManager;
  private final GasStation gasStation;

  public UniswapV2Factory(
    final String contractAddress,
    final Web3j web3j,
    final TransactionManager txManager,
    final GasStation gasStation
  )
    throws IOException {
    log.traceEntry(
      () -> contractAddress,
      () -> web3j,
      () -> txManager,
      () -> gasStation
    );

    this.factory =
      IUniswapV2Factory.load(
        contractAddress,
        web3j,
        txManager,
        new EstimatingGasProvider(contractAddress, web3j, txManager, gasStation)
      );
    this.web3j = web3j;
    this.txManager = txManager;
    this.gasStation = gasStation;

    log.info("Created factory @ address {}", contractAddress);
    log.traceExit();
  }

  @Override
  public TokenPair getPair(
    final String token0Address,
    final String token1Address
  )
    throws Exception {
    log.traceEntry(() -> token0Address, () -> token1Address);
    log.info("Getting pair for {} - {}", token0Address, token1Address);

    return log.traceExit(
      new UniswapV2Pair(
        factory.getPair(token0Address, token1Address).send(),
        web3j,
        txManager,
        gasStation
      )
    );
  }
}
