package sniper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

@Log4j2
public class EstimatingGasProvider implements ContractGasProvider {

  private final String contractAddress;
  private final Web3j web3j;
  private final TransactionManager txManager;
  private final GasStation gasStation;

  public EstimatingGasProvider(
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

    this.contractAddress = contractAddress;
    this.web3j = web3j;
    this.txManager = txManager;
    this.gasStation = gasStation;

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
  public BigInteger getGasLimit(String contractFunc, String data) throws IOException {
    log.traceEntry(() -> contractFunc, () -> data);

    final var transaction = Transaction.createFunctionCallTransaction(
      txManager.getFromAddress(),
      BigInteger.ZERO,
      BigInteger.ZERO,
      BigInteger.ZERO,
      contractAddress,
      data
    );
    final var gasLimit = web3j
      .ethEstimateGas(transaction)
      .send()
      .getAmountUsed();

    log.info(
      "Gas limit for '{}' estimated to be {}",
      contractFunc,
      gasLimit
    );

    return log.traceExit(gasLimit);
  }

  @Override
  public BigInteger getGasLimit() {
    log.traceEntry();
    return log.traceExit(BigInteger.ZERO);
  }
}
