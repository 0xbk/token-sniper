package sniper;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import sniper.uniswap.UniswapV2Factory;
import sniper.uniswap.UniswapV2Router;

@SpringBootApplication
public class Sniper implements CommandLineRunner {

  @Value("${rpcUrl}")
  private String rpcUrl;

  @Value("${token0Address}")
  private String token0Address;

  @Value("${token1Address}")
  private String token1Address;

  @Value("${amountOutToSwap}")
  private double amountOutToSwap;

  @Value("${amountInToSwap}")
  private double amountInToSwap;

  @Value("${transactionCount}")
  private int transactionCount;

  @Value("${uniFactory}")
  private String uniFactory;

  @Value("${uniRouter}")
  private String uniRouter;

  @Value("${publicKey}")
  private String publicKey;

  @Value("${gasLimitMultiplier}")
  private double gasLimitMultiplier;

  @Autowired
  private Web3Service web3;

  @Autowired
  private TransManager transManager;

  @Autowired
  private GasStation gasStation;

  // @Value("${gasPrice}")
  // private BigInteger gasPrice;

  private static final Logger log = LogManager.getLogger();

  public static void main(String[] args) {
    SpringApplication.run(Sniper.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("Loading uniswap factory {}", uniFactory);
    log.info("Loading uniswap router {}", uniRouter);

    final UniswapV2Factory factory = new UniswapV2Factory(
      uniFactory,
      web3,
      transManager
    );
    final UniswapV2Router router = new UniswapV2Router(
      uniRouter,
      web3,
      transManager,
      gasStation
    );
    final Token token0 = new Token(
      token0Address,
      web3,
      transManager,
      gasStation,
      gasLimitMultiplier
    );
    final Token token1 = new Token(
      token1Address,
      web3,
      transManager,
      gasStation,
      gasLimitMultiplier
    );

    final var pairName = String.format(
      "%s-%s",
      token0.symbol(),
      token1.symbol()
    );

    log.info("Getting pair for {}", pairName);

    final var pair = factory.getPair(token0, token1);

    log.info(
      "Pair {} @ address {} has LP symbol {}",
      pairName,
      pair.getContract().getContractAddress(),
      pair.symbol()
    );

    // final var intAmountOutToSwap = BigInteger.valueOf(
    //   (long) (amountOutToSwap * Math.pow(10, token0.decimals().longValue()))
    // );

    // log.info("Approving router to transfer {} tokens", intAmountOutToSwap);

    // token0.approve(router.getContract().getContractAddress(), intAmountToSwap);

    // (long) (amountInToSwap * Math.pow(10, token1.decimals().longValue()))

    final var intAmountInToSwap = BigInteger
      .valueOf((long) Double.parseDouble(String.valueOf(amountInToSwap)))
      .multiply(BigInteger.valueOf(10).pow(token1.decimals().intValue()));
    // final var approvalLimit = BigInteger
    //   .valueOf(999999)
    //   .multiply(BigInteger.valueOf(10).pow(token0.decimals().intValue()));

    // log.info("Approving router to transfer {} tokens", approvalLimit);

    // token0.approve(router.getContract().getContractAddress(), approvalLimit);

    while (true) {
      // Thread.sleep(500);
      Thread.sleep(2500);

      final BigInteger totalSupply = pair.totalSupply();

      log.info(
        "{} pair total supply is {}; waiting to swap {} {} tokens...",
        pairName,
        totalSupply,
        // intAmountToSwap,
        // token0.symbol()
        intAmountInToSwap,
        token1.symbol()
      );

      if (totalSupply.longValue() > 0) {
        break;
      }
    }

    // var remainingAmountToSwap = intAmountOutToSwap;

    // while (remainingAmountToSwap.compareTo(BigInteger.ZERO) > 0) {
    //   var currentAmountToSwap = remainingAmountToSwap;

    //   while (currentAmountToSwap.longValue() > 1) {
    //     log.info(
    //       "Attempting to swap {} {} tokens; {} remaining",
    //       currentAmountToSwap,
    //       token0.symbol(),
    //       remainingAmountToSwap
    //     );

    //     try {
    //       router.swap(token0, token1, remainingAmountToSwap, publicKey);
    //       remainingAmountToSwap =
    //         remainingAmountToSwap.subtract(currentAmountToSwap);

    //       log.info("Swap successful!");

    //       break;
    //     } catch (final ContractCallException e) {
    //       if (e.getMessage().contains("INSUFFICIENT_LIQUIDITY")) {
    //         log.info("Insufficient liquidity");

    //         currentAmountToSwap =
    //           currentAmountToSwap.divide(BigInteger.valueOf(4));
    //       }
    //     }
    //   }
    // }

    // var remainingAmountToSwap = intAmountInToSwap;

    // while (remainingAmountToSwap.compareTo(BigInteger.ZERO) > 0) {
    //   var currentAmountToSwap = remainingAmountToSwap;

    //   while (currentAmountToSwap.longValue() > 1) {
    //     log.info(
    //       "Attempting to swap {} {} tokens; {} remaining",
    //       currentAmountToSwap,
    //       token1.symbol(),
    //       remainingAmountToSwap
    //     );

    //     try {
    //       router.swapTokensForExactTokens(
    //         token0,
    //         token1,
    //         remainingAmountToSwap,
    //         publicKey
    //       );
    //       remainingAmountToSwap =
    //         remainingAmountToSwap.subtract(currentAmountToSwap);

    //       log.info("Swap successful!");

    //       break;
    //     } catch (final ContractCallException e) {
    //       log.error("Tx failed: {}", e.getMessage());

    //       if (e.getMessage().contains("INSUFFICIENT_LIQUIDITY")) {
    //         log.info("Insufficient liquidity");

    //         currentAmountToSwap =
    //           currentAmountToSwap.divide(BigInteger.valueOf(4));
    //       }
    //     }
    //   }
    // }

    log.info("Sending {} transactions", transactionCount);

    final var successfulTransactions = new AtomicInteger(0);
    final var transactionsPending = new AtomicInteger(0);

    while (successfulTransactions.get() != transactionCount) {
      if (transactionsPending.get() != transactionCount) {
        log.info(
          "Attempting to swap {} {} tokens",
          intAmountInToSwap,
          token1.symbol()
        );

        transactionsPending.incrementAndGet();

        router
          .swapTokensForExactTokensAsync(
            token0,
            token1,
            intAmountInToSwap,
            publicKey
          )
          .handle(
            (tx, e) -> {
              if (e == null) {
                log.info("Swap successful, tx: {}", tx.getTransactionHash());

                successfulTransactions.incrementAndGet();
              } else {
                log.info("Tx failed: {}", e.getMessage());

                transactionsPending.decrementAndGet();
              }

              return null;
            }
          );

        Thread.sleep(200);
      }
    }

    log.info("All tx's complete!");
  }
}
