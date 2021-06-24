package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import sniper.config.GasLimitConfig;
import sniper.config.TokenConfig;
import sniper.generic.IERC20Token;

@Component
@Log4j2
public class TokenFactory {

  private static TokenConfig config;
  private static Web3j web3j;
  private static TransactionManager txManager;
  private static GasStation gasStation;
  private static GasLimitConfig gasLimitConfig;

  public TokenFactory(
    @Autowired final TokenConfig config,
    @Autowired final Web3j web3j,
    @Autowired final TransactionManager txManager,
    @Autowired final GasStation gasStation,
    @Autowired final GasLimitConfig gasLimitConfig
  ) {
    log.traceEntry(
      () -> config,
      () -> web3j,
      () -> txManager,
      () -> gasStation,
      () -> gasLimitConfig
    );

    TokenFactory.config = config;
    TokenFactory.web3j = web3j;
    TokenFactory.txManager = txManager;
    TokenFactory.gasStation = gasStation;
    TokenFactory.gasLimitConfig = gasLimitConfig;

    log.traceExit();
  }

  public Token from(final String address) {
    log.traceEntry(() -> address);

    Token token = null;

    if (config.getType() == TokenConfig.Type.ERC20) {
      token =
        new IERC20Token(
          address,
          web3j,
          txManager,
          gasStation,
          GasProvider.get(address, web3j, txManager, gasStation, gasLimitConfig)
        );
    }

    if (token == null) {
      throw new SniperException(
        String.format("Unsupported token type '%s'.", config.getType())
      );
    }

    return log.traceExit(token);
  }
}
