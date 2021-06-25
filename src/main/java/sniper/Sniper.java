package sniper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

@SpringBootApplication
@Log4j2
public class Sniper implements CommandLineRunner {

  @Value("${token.in.contractAddress}")
  private String tokenInAddress;

  @Value("${token.in.amount}")
  private double tokenInAmountHuman;

  @Value("${token.out.contractAddress}")
  private String tokenOutAddress;

  @Value("${token.out.triggerInUsd}")
  private double triggerPriceUsd;

  @Value("${key.public}")
  private String publicKey;

  @Value("${slippageInPercent}")
  private double slippageInPercent;

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

  public static void main(String[] args) {
    SpringApplication.run(Sniper.class, args);
  }

  private BigInteger fromHuman(final double smallValue, final Token token) {
    return BigInteger.valueOf(
      BigDecimal
        .valueOf(smallValue)
        .multiply(BigDecimal.TEN.pow(token.decimals().intValue()))
        .longValue()
    );
  }

  private double toHuman(final BigInteger bigValue, final BigInteger decimals) {
    return (
      bigValue.doubleValue() /
      BigInteger.TEN.pow(decimals.intValue()).doubleValue()
    );
  }

  private String toHumanStr(final BigInteger bigValue, final Token token) {
    final double humanValue =
      bigValue.doubleValue() /
      BigInteger.TEN.pow(token.decimals().intValue()).doubleValue();

    return String.format("%s (%s) %s", bigValue, humanValue, token.symbol());
  }

  @Override
  public void run(String... args) throws Exception {
    final TokenPair pair = factory.getPair(tokenInAddress, tokenOutAddress);
    final Token tokenIn = pair
        .token0()
        .getContractAddress()
        .equals(tokenInAddress)
      ? pair.token0()
      : pair.token1();
    final Token tokenOut = pair
        .token0()
        .getContractAddress()
        .equals(tokenOutAddress)
      ? pair.token0()
      : pair.token1();
    final var amountIn = fromHuman(tokenInAmountHuman, tokenIn);

    log.info(
      "Approving router to transfer {} tokens",
      toHumanStr(amountIn.multiply(BigInteger.TWO), tokenIn)
    );

    // final var approveReceipt = tokenIn
    //   .approve(router.getContractAddress(), amountIn.multiply(BigInteger.TWO))
    //   .send();

    // log.info(
    //   "Approval successful, tx: {}",
    //   approveReceipt.getTransactionHash()
    // );

    BigInteger amountInRemaining = amountIn;

    while (true) {
      Thread.sleep(2000);

      final BigInteger totalSupply = pair.totalSupply();
      final double tokenInUsdPrice = usdPrice.get(tokenOut).doubleValue();
      final boolean priceTriggerHit = tokenInUsdPrice < triggerPriceUsd;

      // log.info(
      //   "{}-{} total supply {}; swapEnabled = {}; max xfer = {}; " +
      //   "${} < ${} = {}; waiting to swap {} tokens...",
      //   pair.token0().symbol(),
      //   pair.token1().symbol(),
      //   totalSupply,
      //   tokenOut.swapEnabled(),
      //   toHumanStr(tokenOut.maxTransferAmount(), tokenOut),
      //   tokenInUsdPrice,
      //   triggerPriceUsd,
      //   tokenInUsdPrice < triggerPriceUsd,
      //   toHumanStr(amountIn, tokenIn)
      // );
      log.info(
        "{}-{} total supply {}; " +
        "${} < ${} = {}; waiting to swap {} tokens...",
        pair.token0().symbol(),
        pair.token1().symbol(),
        totalSupply,
        tokenInUsdPrice,
        triggerPriceUsd,
        tokenInUsdPrice < triggerPriceUsd,
        toHumanStr(amountIn, tokenIn)
      );

      // if (totalSupply.longValue() > 0 && tokenIn.swapEnabled() && priceTriggerHit) {
      if (totalSupply.longValue() > 0 && priceTriggerHit) {
        break;
      }
    }

    log.info("Creating transaction(s) to swap");

    final AtomicInteger txPending = new AtomicInteger(0);
    final AtomicInteger txComplete = new AtomicInteger(0);
    final AtomicInteger txFailures = new AtomicInteger(0);
    final List<Pair<Boolean, TransactionReceipt>> txReceipts = Collections.synchronizedList(
      new LinkedList<>()
    );

    while (amountInRemaining.longValue() > 0) {
      // final BigInteger maxTransfer = tokenOut.maxTransferAmount();
      final BigInteger maxTransfer = BigInteger.valueOf(30000000000000000L);
      BigInteger txAmountIn = router
        .getAmountsIn(maxTransfer, Arrays.asList(tokenIn, tokenOut))
        .get(0);

      if (txAmountIn.longValue() > amountInRemaining.longValue()) {
        txAmountIn = amountInRemaining;

        log.info("Final transaction clamped");

        final BigInteger amountOut = router
          .getAmountsOut(txAmountIn, Arrays.asList(tokenIn, tokenOut))
          .get(1);

        try {
          log.info(
            "Sending tx to swap {} for {}",
            toHumanStr(txAmountIn, tokenIn),
            toHumanStr(amountOut, tokenOut)
          );

          txPending.incrementAndGet();

          router
            .swapExactTokensForTokens(
              tokenIn,
              tokenOut,
              txAmountIn,
              publicKey,
              BigDecimal.valueOf(slippageInPercent)
            )
            .sendAsync()
            .handle(
              (tx, e) -> {
                if (e == null) {
                  txComplete.incrementAndGet();
                } else {
                  txFailures.incrementAndGet();
                }

                if (tx != null) {
                  txReceipts.add(Pair.of(e == null, tx));
                }

                txPending.decrementAndGet();

                return null;
              }
            );

          amountInRemaining = BigInteger.ZERO;
        } catch (final Exception e) {
          if (e.getMessage().contains("INSUFFICIENT_LIQUIDITY")) {
            log.info("Insufficient liquidity");

            Thread.sleep(500);
          } else {
            log.error("Failed to send a transaction", e);
          }
        }
      } else {
        try {
          log.info(
            "Sending tx to swap {} for {}",
            toHumanStr(txAmountIn, tokenIn),
            toHumanStr(maxTransfer, tokenOut)
          );

          txPending.incrementAndGet();

          router
            .swapTokensForExactTokens(
              tokenIn,
              tokenOut,
              maxTransfer,
              publicKey,
              BigDecimal.valueOf(slippageInPercent)
            )
            .sendAsync()
            .handle(
              (tx, e) -> {
                if (e == null) {
                  txComplete.incrementAndGet();
                } else {
                  txFailures.incrementAndGet();
                }

                if (tx != null) {
                  txReceipts.add(Pair.of(e == null, tx));
                }

                txPending.decrementAndGet();

                return null;
              }
            );

          amountInRemaining = amountInRemaining.subtract(txAmountIn);
        } catch (final Exception e) {
          if (e.getMessage().contains("INSUFFICIENT_LIQUIDITY")) {
            log.info("Insufficient liquidity");

            Thread.sleep(500);
          } else {
            log.error("Failed to send a transaction", e);
          }
        }
      }

      log.info(
        "Amount left to spend: {}",
        toHumanStr(amountInRemaining, tokenIn)
      );

      Thread.sleep(100);
    }

    while (txPending.get() != 0) {
      Thread.sleep(1000);
    }

    log.info(
      "All transactions finished: {} complete; {} failures; {} total",
      txComplete.get(),
      txFailures.get(),
      txComplete.get() + txFailures.get()
    );

    txReceipts.forEach(
      txPair ->
        log.info(
          "{}: {}",
          txPair.getLeft(),
          txPair.getRight().getTransactionHash()
        )
    );
  }
}
