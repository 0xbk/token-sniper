package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import sniper.uniswap.UniswapV2Router;

@Configuration
@Log4j2
public class RouterService {

  @Value("${router.contractAddress}")
  private String contractAddress;

  @Autowired
  private Web3j web3j;

  @Autowired
  private TransactionManager txManager;

  @Autowired
  private GasStation gasStation;

  @Bean
  public Router router() throws Exception {
    log.traceEntry();
    return log.traceExit(
      new UniswapV2Router(contractAddress, web3j, txManager, gasStation)
    );
  }
}
