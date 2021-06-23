package sniper;

import java.math.BigInteger;

import org.web3j.generated.contracts.IUniswapV2Pair;

public interface Pair {
  IUniswapV2Pair getContract();
  String symbol() throws Exception;
  BigInteger totalSupply() throws Exception;
}
