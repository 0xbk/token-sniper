package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import sniper.config.RpcConfig;

@Configuration
@Log4j2
public class Web3jService {

  @Bean
  public Web3j web3j(@Autowired final RpcConfig config) {
    log.traceEntry(() -> config);

    final var web3j = Web3j.build(new HttpService(config.getUrl()));

    log.info("Created web3j service @ rpc url {}", config.getUrl());

    return log.traceExit(web3j);
  }
}
