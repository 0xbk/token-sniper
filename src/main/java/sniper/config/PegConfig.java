package sniper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "peg")
@Getter
@Setter
public class PegConfig {

  private String usd1DollarAddress;
}