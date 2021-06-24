package sniper;

import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class FactoryService {

  @Bean
  public Factory factory(@Autowired final Router router) throws IOException {
    log.traceEntry();
    return log.traceExit(router.getFactory());
  }
}
