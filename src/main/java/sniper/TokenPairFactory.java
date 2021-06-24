package sniper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class TokenPairFactory {

  private static Factory factory;

  public static TokenPair from(final Token token0, final Token token1) {
    log.traceEntry(() -> token0, () -> token1);
    return log.traceExit(factory.getPair(token0, token1));
  }

  TokenPairFactory(@Autowired final Factory factory) {
    log.traceEntry();

    TokenPairFactory.factory = factory;

    log.traceExit();
  }
}
