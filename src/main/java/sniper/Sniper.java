package sniper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.web3j.protocol.Web3j;

@SpringBootApplication
@Log4j2
public class Sniper implements CommandLineRunner {

  @Value("${token.out.contractAddress}")
  private String tokenOutAddress;

  @Value("${token.in.contractAddress}")
  private String tokenInAddress;

  // @Value("${token.out.amount}")
  // private double amountOutToSwap;

  // @Value("${amountInToSwap}")
  // private double amountInToSwap;

  // @Value("${transactionCount}")
  // private int transactionCount;

  // @Value("${uniFactory}")
  // private String uniFactory;

  // @Value("${uniRouter}")
  // private String uniRouter;

  // @Value("${publicKey}")
  // private String publicKey;

  // @Value("${gasLimitMultiplier}")
  // private double gasLimitMultiplier;

  // @Autowired
  // private Web3j web3;

  // @Autowired
  // private TransManager transManager;

  @Autowired
  private Web3j web3j;

  @Autowired
  private TxManager txManager;

  @Autowired
  private GasStation gasStation;

  @Autowired
  private PollingService pollingService;

  @Autowired
  private Router router;

  @Autowired
  private Factory factory;

  @Autowired
  private UsdPriceService usdPrice;

  @Autowired
  private Environment env;

  // // @Value("${gasPrice}")
  // // private BigInteger gasPrice;

  // private static final Logger log = LogManager.getLogger();

  public static void main(String[] args) {
    SpringApplication.run(Sniper.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    // Configure property combinations and for what snipe types they mean.

    final List<Pair<String, List<String>>> configurations = new LinkedList<>();

    configurations.add(
      Pair.of(
        "Spend the defined amount of tokens to buy as many tokens as possible",
        Arrays.asList("token.out.amount")
      )
    );

    // Get matching configurations.

    final List<Pair<String, List<String>>> matchingConfigurations = new LinkedList<>();

    for (final var configuration : configurations) {
      if (configuration.getRight().stream().allMatch(env::containsProperty)) {
        matchingConfigurations.add(configuration);
      }
    }

    if (matchingConfigurations.isEmpty()) {
      log.error("No matching configurations for the defined properties.");
      return;
    } else if (matchingConfigurations.size() > 1) {
      log.error("Multiple matching configurations for the defined properties.");
      return;
    }

    // We're all good.

    final var configuration = matchingConfigurations.get(0);

    log.info("Running in '{}' mode", configuration.getLeft().toLowerCase());

    final TokenPair pair = factory.getPair(tokenOutAddress, tokenInAddress);
    final Token tokenOut = pair
        .token0()
        .getContractAddress()
        .equals(tokenOutAddress)
      ? pair.token0()
      : pair.token1();
    final var amountOutStr = env.getProperty("token.out.amount");
    final var amountOut = BigInteger.valueOf(
      BigDecimal
        .valueOf(Double.parseDouble(amountOutStr))
        .multiply(BigDecimal.TEN.pow(tokenOut.decimals().intValue()))
        .longValue()
    );

    log.info(
      "Approving router to transfer {} {} tokens",
      amountOutStr,
      tokenOut.symbol()
    );

    // final var approveReceipt = tokenOut
    //   .approve(router.getContractAddress(), amountOut)
    //   .send();

    // log.info(
    //   "Approval successful, tx: {}",
    //   approveReceipt.getTransactionHash()
    // );

    //   while (true) {
    //     // Thread.sleep(500);
    //     Thread.sleep(2500);

    //     final BigInteger totalSupply = pair.totalSupply();

    //     log.info(
    //       "{} pair total supply is {}; waiting to swap {} {} tokens...",
    //       pairName,
    //       totalSupply,
    //       // intAmountToSwap,
    //       // token0.symbol()
    //       intAmountInToSwap,
    //       token1.symbol()
    //     );

    //     if (totalSupply.longValue() > 0) {
    //       break;
    //     }
    //   }

    //   // var remainingAmountToSwap = intAmountOutToSwap;

    //   // while (remainingAmountToSwap.compareTo(BigInteger.ZERO) > 0) {
    //   //   var currentAmountToSwap = remainingAmountToSwap;

    //   //   while (currentAmountToSwap.longValue() > 1) {
    //   //     log.info(
    //   //       "Attempting to swap {} {} tokens; {} remaining",
    //   //       currentAmountToSwap,
    //   //       token0.symbol(),
    //   //       remainingAmountToSwap
    //   //     );

    //   //     try {
    //   //       router.swap(token0, token1, remainingAmountToSwap, publicKey);
    //   //       remainingAmountToSwap =
    //   //         remainingAmountToSwap.subtract(currentAmountToSwap);

    //   //       log.info("Swap successful!");

    //   //       break;
    //   //     } catch (final ContractCallException e) {
    //   //       if (e.getMessage().contains("INSUFFICIENT_LIQUIDITY")) {
    //   //         log.info("Insufficient liquidity");

    //   //         currentAmountToSwap =
    //   //           currentAmountToSwap.divide(BigInteger.valueOf(4));
    //   //       }
    //   //     }
    //   //   }
    //   // }

    //   // var remainingAmountToSwap = intAmountInToSwap;

    //   // while (remainingAmountToSwap.compareTo(BigInteger.ZERO) > 0) {
    //   //   var currentAmountToSwap = remainingAmountToSwap;

    //   //   while (currentAmountToSwap.longValue() > 1) {
    //   //     log.info(
    //   //       "Attempting to swap {} {} tokens; {} remaining",
    //   //       currentAmountToSwap,
    //   //       token1.symbol(),
    //   //       remainingAmountToSwap
    //   //     );

    //   //     try {
    //   //       router.swapTokensForExactTokens(
    //   //         token0,
    //   //         token1,
    //   //         remainingAmountToSwap,
    //   //         publicKey
    //   //       );
    //   //       remainingAmountToSwap =
    //   //         remainingAmountToSwap.subtract(currentAmountToSwap);

    //   //       log.info("Swap successful!");

    //   //       break;
    //   //     } catch (final ContractCallException e) {
    //   //       log.error("Tx failed: {}", e.getMessage());

    //   //       if (e.getMessage().contains("INSUFFICIENT_LIQUIDITY")) {
    //   //         log.info("Insufficient liquidity");

    //   //         currentAmountToSwap =
    //   //           currentAmountToSwap.divide(BigInteger.valueOf(4));
    //   //       }
    //   //     }
    //   //   }
    //   // }

    //   log.info("Sending {} transactions", transactionCount);

    //   final var successfulTransactions = new AtomicInteger(0);
    //   final var transactionsPending = new AtomicInteger(0);

    //   while (successfulTransactions.get() != transactionCount) {
    //     if (transactionsPending.get() != transactionCount) {
    //       log.info(
    //         "Attempting to swap {} {} tokens",
    //         intAmountInToSwap,
    //         token1.symbol()
    //       );

    //       transactionsPending.incrementAndGet();

    //       router
    //         .swapTokensForExactTokensAsync(
    //           token0,
    //           token1,
    //           intAmountInToSwap,
    //           publicKey
    //         )
    //         .handle(
    //           (tx, e) -> {
    //             if (e == null) {
    //               log.info("Swap successful, tx: {}", tx.getTransactionHash());

    //               successfulTransactions.incrementAndGet();
    //             } else {
    //               log.info("Tx failed: {}", e.getMessage());

    //               transactionsPending.decrementAndGet();
    //             }

    //             return null;
    //           }
    //         );

    //       Thread.sleep(200);
    //     }
    //   }

    //   log.info("All tx's complete!");
  }
}
