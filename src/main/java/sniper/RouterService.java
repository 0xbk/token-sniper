package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import sniper.config.GasLimitConfig;
import sniper.config.RouterConfig;
import sniper.config.SwapConfig;
import sniper.uniswap.UniswapV2Router;

@Configuration
@Log4j2
public class RouterService {

  @Bean
  public Router router(
    @Autowired final RouterConfig config,
    @Autowired final SwapConfig swapConfig,
    @Autowired final Web3j web3j,
    @Autowired final TransactionManager txManager,
    @Autowired final GasStation gasStation,
    @Autowired final GasLimitConfig gasLimitConfig,
    @Autowired final TokenFactory token,
    @Autowired final Converter converter
  ) {
    log.traceEntry();
    Router router = null;

    if (config.getType() == RouterConfig.Type.UNISWAPV2) {
      router =
        new UniswapV2Router(
          config.getAddress(),
          web3j,
          txManager,
          gasStation,
          GasProvider.get(
            config.getAddress(),
            web3j,
            txManager,
            gasStation,
            gasLimitConfig
          ),
          token,
          converter,
          swapConfig
        );
    }

    if (router == null) {
      throw new SniperException(
        String.format("Unsupported router type '%s'.", config.getType())
      );
    }

    return log.traceExit(router);
  }
}
