package sniper;

import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import sniper.config.GasLimitConfig;

public interface GasProvider {
  static ContractGasProvider get(
    final String contractAddress,
    final Web3j web3j,
    final TransactionManager txManager,
    final GasStation gasStation,
    final GasLimitConfig gasLimitConfig
  ) {
    return new EstimatingGasProvider(
      contractAddress,
      web3j,
      txManager,
      gasStation,
      gasLimitConfig
    );
  }
}
