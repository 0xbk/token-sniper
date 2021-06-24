package sniper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

@Log4j2
public class StaticGasStation implements GasStation {

  private BigInteger priceInWei;

  public StaticGasStation(
    final BigDecimal priceInGwei,
    final BigDecimal priceMultiplier
  ) {
    log.traceEntry(() -> priceInGwei, () -> priceMultiplier);

    Objects.requireNonNull(priceInGwei);
    Objects.requireNonNull(priceMultiplier);

    final var multipliedPriceInGwei = priceInGwei.multiply(priceMultiplier);
    priceInWei =
      BigInteger.valueOf(
        Convert.toWei(multipliedPriceInGwei, Unit.GWEI).longValue()
      );

    log.info(
      "Created static gas station; gas price set to {} Gwei / {} Wei using " +
      "multiplier of {}",
      multipliedPriceInGwei,
      priceInWei,
      priceMultiplier
    );
    log.traceExit();
  }

  @Override
  public BigInteger getPriceInWei() {
    log.traceEntry();
    return log.traceExit(priceInWei);
  }
}
