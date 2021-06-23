package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Component
@Log4j2
public class Web3Service {

  private final Web3j web3;

  public Web3Service(@Value("${rpcUrl}") final String rpcUrl) {
    web3 = Web3j.build(new HttpService(rpcUrl));

    log.info("Created web3 service with RPC url '{}'", rpcUrl);
  }

  public Web3j get() {
    return web3;
  }
}
