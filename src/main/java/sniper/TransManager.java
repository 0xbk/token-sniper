package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;

@Component
@Log4j2
public class TransManager {

  private final TransactionManager manager;

  public TransManager(
    @Autowired final Web3Service web3service,
    @Value("${privateKey}") final String privateKey,
    @Value("${chainId}") final long chainId
  ) {
    this.manager =
      new RawTransactionManager(
        web3service.get(),
        Credentials.create(privateKey),
        chainId
      );

    log.info(
      "Created transaction manager with private key '{}' and chain id {}",
      privateKey,
      chainId
    );
  }

  public TransactionManager get() {
    return manager;
  }
}
