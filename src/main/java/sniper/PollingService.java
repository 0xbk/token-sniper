package sniper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class PollingService {

  private Duration duration = Duration.ofSeconds(2);
  private ScheduledExecutorService executor = Executors.newScheduledThreadPool(
    20
  );
  private final List<Runnable> runnables = new ArrayList<>();

  public Duration getDuration() {
    log.traceEntry();
    return log.traceExit(duration);
  }

  public void setDuration(final Duration value) {
    log.traceEntry(() -> value);

    Objects.requireNonNull(value);

    if (!duration.equals(value)) {
      log.debug("Duration changed from {} to {}", duration, value);

      duration = value;
      executor.shutdownNow();
      executor = Executors.newScheduledThreadPool(20);

      for (Runnable runnable : runnables) {
        executor.scheduleAtFixedRate(
          runnable,
          0,
          duration.toMillis(),
          TimeUnit.MILLISECONDS
        );
      }
    }

    log.traceExit();
  }

  public void addExecutor(final Runnable runnable) {
    log.traceEntry(() -> runnable);

    Objects.requireNonNull(runnable);

    log.debug("Adding runnable {}", runnable);

    runnables.add(runnable);
    executor.scheduleAtFixedRate(
      runnable,
      0,
      getDuration().toSeconds(),
      TimeUnit.SECONDS
    );

    log.traceExit();
  }
}
