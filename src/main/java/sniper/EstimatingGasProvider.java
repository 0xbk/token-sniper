package sniper;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.extern.log4j.Log4j2;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import sniper.config.GasLimitConfig;

@Log4j2
public class EstimatingGasProvider implements ContractGasProvider {

  private final String contractAddress;
  private final Web3j web3j;
  private final TransactionManager txManager;
  private final GasStation gasStation;
  private final GasLimitConfig gasLimitConfig;

  public EstimatingGasProvider(
    final String contractAddress,
    final Web3j web3j,
    final TransactionManager txManager,
    final GasStation gasStation,
    final GasLimitConfig gasLimitConfig
  ) {
    log.traceEntry(
      () -> contractAddress,
      () -> web3j,
      () -> txManager,
      () -> gasStation,
      () -> gasLimitConfig
    );

    this.contractAddress = contractAddress;
    this.web3j = web3j;
    this.txManager = txManager;
    this.gasStation = gasStation;
    this.gasLimitConfig = gasLimitConfig;

    log.traceExit();
  }

  @Override
  public BigInteger getGasPrice(String contractFunc, String data) {
    log.traceEntry(() -> contractFunc);
    return log.traceExit(gasStation.getPriceInWei());
  }

  @Override
  public BigInteger getGasPrice() {
    log.traceEntry();
    return log.traceExit(gasStation.getPriceInWei());
  }

  @Override
  public BigInteger getGasLimit(String contractFunc, String data) {
    log.traceEntry(() -> contractFunc, () -> data);

    final var transaction = Transaction.createFunctionCallTransaction(
      txManager.getFromAddress(),
      BigInteger.ZERO,
      BigInteger.ZERO,
      BigInteger.ZERO,
      contractAddress,
      data
    );

    BigInteger gasLimit;

    try {
      gasLimit = web3j.ethEstimateGas(transaction).send().getAmountUsed();
    } catch (final Exception e) {
      gasLimit = BigInteger.valueOf(gasLimitConfig.getStaticValue());

      log.error(
        "Failed to get the gas limit estimate, using static value {}",
        gasLimitConfig.getStaticValue()
      );
    }

    final var gasLimitWithMultiplier = new BigDecimal(gasLimit)
      .multiply(BigDecimal.valueOf(gasLimitConfig.getMultiplier()))
      .toBigInteger();

    log.info(
      "Gas limit for '{}' estimated to be {}; {} with multiplier of {}",
      contractFunc,
      gasLimit,
      gasLimitWithMultiplier,
      gasLimitConfig.getMultiplier()
    );

    return log.traceExit(gasLimitWithMultiplier);
  }

  @Override
  public BigInteger getGasLimit() {
    log.traceEntry();
    return log.traceExit(BigInteger.ZERO);
  }
}
