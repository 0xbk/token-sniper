package sniper.uniswap;

import java.math.BigInteger;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.web3j.generated.contracts.IUniswapV2Pair;
import org.web3j.tx.gas.DefaultGasProvider;
import sniper.Pair;
import sniper.TransManager;
import sniper.Web3Service;

public class UniswapV2Pair implements Pair {

  private final IUniswapV2Pair pair;

  public UniswapV2Pair(
    final String contractAddress,
    final Web3Service web3,
    final TransManager transManager
  ) {
    this.pair =
      IUniswapV2Pair.load(
        contractAddress,
        web3.get(),
        transManager.get(),
        new DefaultGasProvider()
      );
  }

  public IUniswapV2Pair getContract() {
    return pair;
  }

  public String symbol() throws Exception {
    return pair.symbol().send();
  }

  public BigInteger totalSupply() throws Exception {
    return pair.totalSupply().send();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
