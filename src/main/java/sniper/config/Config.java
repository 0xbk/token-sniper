package sniper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
@Getter
@Setter
public class Config {

  public enum Mode {
    APPROVE_TOKENS,
    SINGLE_SWAP_ALL_TOKEN_IN,
    MULTI_SWAP_TOKEN_OUT_TX_LIMIT,
    MULTI_SWAP_TOKEN_IN_TX_LIMIT
  }

  private boolean dryRun;
  private Mode mode;
}
