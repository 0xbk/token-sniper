package sniper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "router")
@Getter
@Setter
public class RouterConfig {

  public enum Type {
   UNISWAPV2 
  }

  private String address;
  private Type type;
}
