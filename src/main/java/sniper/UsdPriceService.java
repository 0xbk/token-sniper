package sniper;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sniper.config.PegConfig;

@Component
@Log4j2
public class UsdPriceService {

  private final Token usd1;
  private final Router router;

  public UsdPriceService(
    @Autowired final PegConfig config,
    @Autowired final Router router,
    @Autowired final TokenFactory token
  ) {
    log.traceEntry(() -> config, () -> router);

    this.usd1 = token.from(config.getUsd1DollarAddress());
    this.router = router;

    log.info("Created USD price service pegged to {}", this.usd1.getSymbol());
    log.traceExit();
  }

  public BigDecimal get(final Token token) {
    log.traceEntry(() -> token);

    if (usd1.getAddress().equals(token.getAddress())) {
      return BigDecimal.ONE;
    }

    final BigInteger amountOut = router.getAmountOut(
      token,
      usd1,
      BigInteger.ONE.multiply(
        BigInteger.TEN.pow(token.getDecimals().intValue())
      )
    );

    return log.traceExit(
      BigDecimal
        .valueOf(amountOut.longValue())
        .divide(BigDecimal.TEN.pow(usd1.getDecimals().intValue()))
    );
  }
}
