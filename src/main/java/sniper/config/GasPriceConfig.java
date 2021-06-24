package sniper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gas.price")
@Getter
@Setter
public class GasPriceConfig {

  private double multiplier;
  private double staticInGwei;
}
