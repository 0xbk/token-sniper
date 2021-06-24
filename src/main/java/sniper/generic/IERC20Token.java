package sniper.generic;

import java.math.BigInteger;
import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.IERC20;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import sniper.EstimatingGasProvider;
import sniper.GasStation;
import sniper.Token;

@Log4j2
public class IERC20Token implements Token {

  private final IERC20 ierc20;
  private final BigInteger decimals;
  private final String symbol;

  public IERC20Token(
    final String contractAddress,
    final Web3j web3j,
    final TransactionManager txManager,
    final GasStation gasStation
  )
    throws Exception {
    log.traceEntry(
      () -> contractAddress,
      () -> web3j,
      () -> txManager,
      () -> gasStation
    );

    this.ierc20 =
      IERC20.load(
        contractAddress,
        web3j,
        txManager,
        new EstimatingGasProvider(contractAddress, web3j, txManager, gasStation)
      );
    this.decimals = this.ierc20.decimals().send();
    this.symbol = this.ierc20.symbol().send();

    log.info("Created {} token @ address {}", symbol(), getContractAddress());

    log.traceExit();
  }

  @Override
  public String getContractAddress() {
    log.traceEntry();
    return log.traceExit(ierc20.getContractAddress());
  }

  @Override
  public RemoteFunctionCall<TransactionReceipt> approve(
    final String spender,
    final BigInteger amount
  ) {
    log.traceEntry(() -> spender, () -> amount);
    log.info("Approving {} to spend {} {}", spender, amount, symbol());

    return log.traceExit(ierc20.approve(spender, amount));
  }

  @Override
  public BigInteger decimals() {
    log.traceEntry();
    return log.traceExit(decimals);
  }

  @Override
  public String symbol() {
    log.traceEntry();
    return log.traceExit(symbol);
  }
}
