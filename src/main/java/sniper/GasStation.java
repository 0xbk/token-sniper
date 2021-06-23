package sniper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

@Component
@Log4j2
public class GasStation {

  private final String gasStationUrl;
  private final String gasSpeed;
  private final int gasPriceMultiplier;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
    1
  );
  private BigDecimal gasPrice;

  public GasStation(
    @Value("${gasStationUrl}") final String gasStationUrl,
    @Value("${gasSpeed}") final String gasSpeed,
    @Value("${gasPriceMultiplier}") final int gasPriceMultiplier,
    @Autowired Environment env
  ) {
    this.gasStationUrl = gasStationUrl;
    this.gasSpeed = gasSpeed;
    this.gasPriceMultiplier = gasPriceMultiplier;

    if (env.containsProperty("gasPriceStatic")) {
      this.gasPrice =
        BigDecimal.valueOf(
          Double.parseDouble(env.getProperty("gasPriceStatic"))
        );
    } else {
      this.gasPrice = retrieveGasPrice();

      executor.scheduleAtFixedRate(
        () -> this.gasPrice = retrieveGasPrice(),
        10,
        10,
        TimeUnit.SECONDS
      );
    }

    log.info("Gas price set to {} Gwei", getGasPrice());
  }

  private BigDecimal retrieveGasPrice() {
    BigDecimal retrievedGasPrice = BigDecimal.ZERO;
    final ObjectMapper mapper = new ObjectMapper();

    try {
      log.info(
        "Retrieving {} gas price from gas station at url '{}'",
        gasSpeed,
        gasStationUrl
      );

      final Map<String, Object> gasValues = mapper.readValue(
        new URL(gasStationUrl),
        Map.class
      );

      retrievedGasPrice =
        BigDecimal.valueOf(
          Double.parseDouble(gasValues.get(gasSpeed).toString())
        );

      log.info("Retrieved {} gas price of {}", gasSpeed, retrievedGasPrice);
    } catch (IOException e) {
      log.error(
        "Failed to retrieve gas price, defaulting to {}",
        retrievedGasPrice,
        e
      );
    }

    return retrievedGasPrice;
  }

  public BigInteger getGasPrice() {
    return BigInteger.valueOf(
      Convert.toWei(gasPrice, Unit.GWEI).longValue() * gasPriceMultiplier
    );
  }
}
