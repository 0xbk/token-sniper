package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;

@Component
@Log4j2
public class TxManager extends RawTransactionManager {

  public TxManager(
    @Autowired final Web3j web3j,
    @Value("${key.private}") final String privateKey,
    @Value("${rpc.chainId}") final long chainId
  ) {
    super(web3j, Credentials.create(privateKey), chainId);
    log.traceEntry(() -> web3j, () -> privateKey, () -> chainId);
    log.info(
      "Created transaction manager with private key {} and chain id {}",
      privateKey,
      chainId
    );
    log.traceExit();
  }
}
