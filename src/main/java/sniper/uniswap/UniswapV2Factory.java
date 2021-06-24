package sniper.uniswap;

import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.IUniswapV2Factory;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import sniper.Factory;
import sniper.GasStation;
import sniper.SniperException;
import sniper.Token;
import sniper.TokenFactory;
import sniper.TokenPair;

@Log4j2
public class UniswapV2Factory implements Factory {

  private final IUniswapV2Factory factory;
  private final Web3j web3j;
  private final TransactionManager txManager;
  private final GasStation gasStation;
  private final ContractGasProvider gasProvider;
  private final TokenFactory token;

  public UniswapV2Factory(
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
      () -> gasProvider,
      () -> token
    );

    this.factory =
      IUniswapV2Factory.load(contractAddress, web3j, txManager, gasProvider);
    this.web3j = web3j;
    this.txManager = txManager;
    this.gasStation = gasStation;
    this.gasProvider = gasProvider;
    this.token = token;

    log.info("Created factory @ address {}", contractAddress);
    log.traceExit();
  }

  @Override
  public TokenPair getPair(final Token token0, final Token token1) {
    log.traceEntry(() -> token0, () -> token1);

    try {
      return new UniswapV2Pair(
        factory.getPair(token0.getAddress(), token1.getAddress()).send(),
        web3j,
        txManager,
        gasStation,
        gasProvider,
        token
      );
    } catch (final Exception e) {
      throw new SniperException("Failed to get a pair.", e);
    }
  }
}
