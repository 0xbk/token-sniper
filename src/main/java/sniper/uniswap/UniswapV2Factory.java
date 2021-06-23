package sniper.uniswap;

import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.IUniswapV2Factory;
import org.web3j.tx.gas.DefaultGasProvider;
import sniper.Factory;
import sniper.Pair;
import sniper.Token;
import sniper.TransManager;
import sniper.Web3Service;

@Log4j2
public class UniswapV2Factory implements Factory {

  private final IUniswapV2Factory factory;
  private final Web3Service web3;
  private final TransManager transManager;

  public UniswapV2Factory(
    final String contractAddress,
    final Web3Service web3,
    final TransManager transManager
  ) {
    this.factory =
      IUniswapV2Factory.load(
        contractAddress,
        web3.get(),
        transManager.get(),
        new DefaultGasProvider()
      );
    this.web3 = web3;
    this.transManager = transManager;
  }

  public Pair getPair(final Token token0, final Token token1) throws Exception {
    return new UniswapV2Pair(
      factory
        .getPair(
          token0.getContract().getContractAddress(),
          token1.getContract().getContractAddress()
        )
        .send(),
      web3,
      transManager
    );
  }
}
