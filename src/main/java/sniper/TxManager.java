package sniper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;
import sniper.config.Config;
import sniper.config.KeyConfig;
import sniper.config.RpcConfig;

@Component
@Log4j2
public class TxManager extends RawTransactionManager {

  private Config config;
  private Optional<BigInteger> nonceOverride = Optional.empty();

  public TxManager(
    @Autowired final Config config,
    @Autowired final KeyConfig keyConfig,
    @Autowired final RpcConfig rpcConfig,
    @Autowired final Web3j web3j
  ) {
    super(
      web3j,
      Credentials.create(keyConfig.getPrivateKey()),
      rpcConfig.getChainId()
    );
    log.traceEntry(() -> keyConfig, () -> rpcConfig, () -> web3j);

    this.config = config;

    log.info(
      "Created" +
      (config.isDryRun() ? " dry run" : "") +
      " transaction manager with private key {} and chain id {}",
      keyConfig.getPrivateKey(),
      rpcConfig.getChainId()
    );
    log.traceExit();
  }

  public void enableManualNonceIncrement(boolean enabled) {
    log.traceEntry(() -> enabled);

    if (enabled) {
      try {
        nonceOverride = Optional.of(getNonce());
      } catch (final IOException e) {
        throw new SniperException("Failed to get the nonce.", e);
      }
    } else {
      nonceOverride = Optional.empty();
    }

    log.traceExit();
  }

  @Override
  protected BigInteger getNonce() throws IOException {
    log.traceEntry();

    BigInteger nonce;

    if (nonceOverride.isPresent()) {
      log.info("Using nonce override value of {}", nonceOverride.get());

      nonce = nonceOverride.get();
      nonceOverride = Optional.of(nonceOverride.get().add(BigInteger.ONE));
    } else {
      nonce = super.getNonce();
    }

    return log.traceExit(nonce);
  }

  @Override
  protected TransactionReceipt executeTransaction(
    BigInteger gasPrice,
    BigInteger gasLimit,
    String to,
    String data,
    BigInteger value,
    boolean constructor
  )
    throws IOException, TransactionException {
    log.traceEntry(
      () -> gasPrice,
      () -> gasLimit,
      () -> to,
      () -> data,
      () -> value,
      () -> constructor
    );

    log.info(
      "{} tx to {} with gas price {} Gwei and limit {}",
      config.isDryRun() ? "Not sending" : "Sending",
      to,
      Convert.fromWei(BigDecimal.valueOf(gasPrice.doubleValue()), Unit.GWEI),
      gasLimit
    );

    TransactionReceipt receipt;

    if (config.isDryRun()) {
      receipt = new TransactionReceipt();
    } else {
      receipt =
        super.executeTransaction(
          gasPrice,
          gasLimit,
          to,
          data,
          value,
          constructor
        );
    }

    return log.traceExit(receipt);
  }

  @Override
  protected TransactionReceipt executeTransactionEIP1559(
    BigInteger gasPremium,
    BigInteger feeCap,
    BigInteger gasLimit,
    String to,
    String data,
    BigInteger value,
    boolean constructor
  )
    throws IOException, TransactionException {
    log.traceEntry(
      () -> gasPremium,
      () -> feeCap,
      () -> gasLimit,
      () -> to,
      () -> data,
      () -> value,
      () -> constructor
    );
    log.info(
      "{} tx to {} with gas premium {} Gwei and limit {}",
      config.isDryRun() ? "Not sending" : "Sending",
      to,
      Convert.fromWei(BigDecimal.valueOf(gasPremium.doubleValue()), Unit.GWEI),
      gasLimit
    );

    TransactionReceipt receipt;

    if (config.isDryRun()) {
      receipt = new TransactionReceipt();
    } else {
      receipt =
        super.executeTransactionEIP1559(
          gasPremium,
          feeCap,
          gasLimit,
          to,
          data,
          value,
          constructor
        );
    }

    return log.traceExit(receipt);
  }
}
