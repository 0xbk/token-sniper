package sniper.config;

import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gas.limit")
@Getter
@Setter
public class GasLimitConfig {

  private double multiplier;
  private Optional<Integer> staticValue;
}
