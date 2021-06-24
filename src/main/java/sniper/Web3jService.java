package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
@Log4j2
public class Web3jService {

  @Bean
  public Web3j web3j(@Value("${rpc.url}") final String rpcUrl) {
    log.traceEntry(() -> rpcUrl);

    final var web3j = Web3j.build(new HttpService(rpcUrl));

    log.info("Created web3j service @ rpc url {}", rpcUrl);

    return log.traceExit(web3j);
  }
}
