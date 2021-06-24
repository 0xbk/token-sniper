package sniper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

@Log4j2
public class DynamicGasStation implements GasStation {

  private BigInteger priceInWei;

  public DynamicGasStation(
    final String gasStationUrl,
    final String gasStationSpeed,
    final BigDecimal priceMultiplier,
    final PollingService pollingService
  ) {
    log.traceEntry(
      () -> gasStationUrl,
      () -> gasStationSpeed,
      () -> priceMultiplier,
      () -> pollingService
    );

    Objects.nonNull(gasStationUrl);
    Objects.nonNull(gasStationSpeed);
    Objects.nonNull(priceMultiplier);
    Objects.nonNull(pollingService);

    priceInWei =
      retrievePriceInWei(gasStationUrl, gasStationSpeed, priceMultiplier);

    pollingService.addExecutor(
      () -> {
        this.priceInWei =
          retrievePriceInWei(gasStationUrl, gasStationSpeed, priceMultiplier);
      }
    );

    log.info("Created dyamic gas station @ {}", gasStationUrl);
    log.traceExit();
  }

  private BigInteger retrievePriceInWei(
    final String gasStationUrl,
    final String gasSpeed,
    final BigDecimal priceMultiplier
  ) {
    log.traceEntry(() -> gasStationUrl, () -> gasSpeed, () -> priceMultiplier);

    final ObjectMapper mapper = new ObjectMapper();

    try {
      final Map<String, Object> gasValues = mapper.readValue(
        new URL(gasStationUrl),
        Map.class
      );
      final var retrievedPriceInGwei = BigDecimal
        .valueOf(Double.parseDouble(gasValues.get(gasSpeed).toString()))
        .multiply(priceMultiplier);
      final var retrievedPriceInWei = BigInteger.valueOf(
        Convert.toWei(retrievedPriceInGwei, Unit.GWEI).longValue()
      );

      log.debug(
        "Retrieved {} dynamic gas price of {} Gwei / {} Wei using multiplier of {}",
        gasSpeed,
        retrievedPriceInGwei,
        retrievedPriceInWei,
        priceMultiplier
      );

      return log.traceExit(retrievedPriceInWei);
    } catch (final IOException e) {
      throw new SniperException(
        "Failed to retrieve price from the gas station.",
        e
      );
    }
  }

  @Override
  public BigInteger getPriceInWei() {
    return priceInWei;
  }
}
