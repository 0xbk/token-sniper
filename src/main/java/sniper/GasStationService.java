package sniper;

import java.io.IOException;
import java.math.BigDecimal;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@Log4j2
public class GasStationService {

  private static final String KEY_PRICE_MULTIPLIER = "gas.price.multiplier";
  private static final String KEY_PRICE_STATIC = "gas.price.staticInGwei";
  private static final String KEY_STATION_URL = "gas.station.url";
  private static final String KEY_STATION_SPEED = "gas.station.speed";

  @Bean
  public GasStation gasStation(
    @Autowired final Environment env,
    @Autowired final PollingService pollingService
  )
    throws IOException {
    log.traceEntry(() -> env, () -> pollingService);

    GasStation gasStation;
    final BigDecimal priceMultiplier = env.containsProperty(
        KEY_PRICE_MULTIPLIER
      )
      ? BigDecimal.valueOf(
        Double.parseDouble(env.getProperty(KEY_PRICE_MULTIPLIER))
      )
      : BigDecimal.ONE;

    if (env.containsProperty(KEY_PRICE_STATIC)) {
      gasStation =
        new StaticGasStation(
          BigDecimal.valueOf(
            Double.parseDouble(env.getProperty(KEY_PRICE_STATIC))
          ),
          priceMultiplier
        );
    } else if (
      env.containsProperty(KEY_STATION_URL) &&
      env.containsProperty(KEY_STATION_SPEED)
    ) {
      gasStation =
        new DynamicGasStation(
          env.getProperty(KEY_STATION_URL),
          env.getProperty(KEY_STATION_SPEED),
          priceMultiplier,
          pollingService
        );
    } else {
      throw log.throwing(
        new IllegalArgumentException(
          String.format(
            "Either (%s) or (%s and %s) must be defined in application.properties",
            KEY_PRICE_STATIC,
            KEY_STATION_URL,
            KEY_STATION_SPEED
          )
        )
      );
    }

    return log.traceExit(gasStation);
  }
}
