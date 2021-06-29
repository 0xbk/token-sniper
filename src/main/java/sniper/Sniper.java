package sniper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import sniper.config.Config;
import sniper.config.Config.Mode;
import sniper.config.LpConfig;
import sniper.config.SwapInConfig;
import sniper.config.SwapOutConfig;

@SpringBootApplication
@Log4j2
public class Sniper implements CommandLineRunner {

  @Autowired
  private Config config;

  @Autowired
  private LpConfig lpConfig;

  @Autowired
  private SwapOutConfig swapOutConfig;

  @Autowired
  private SwapInConfig swapInConfig;

  @Autowired
  private TokenFactory token;

  @Autowired
  private Router router;

  @Autowired
  private TxManager txManager;

  @Autowired
  private Converter converter;

  public static void main(String[] args) {
    SpringApplication.run(Sniper.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    final var inToken = token.from(swapInConfig.getToken());
    final var outToken = token.from(swapOutConfig.getToken());
    final var inAmount = converter.fromHuman(
      swapInConfig.getAmount(),
      inToken.getDecimals()
    );

    if (config.getMode() == Mode.APPROVE_TOKENS) {
      approveTokens(inToken, outToken, inAmount);

      return;
    } else {
      approveToken(inToken, inAmount.multiply(BigInteger.TWO));
    }

    final long loopPauseInMillis = 000;
    final var lpToken0 = token.from(lpConfig.getToken0());
    final var lpToken1 = token.from(lpConfig.getToken1());
    TokenPair lpPair = null;

    while (lpPair == null) {
      try {
        lpPair = TokenPair.from(lpToken0, lpToken1);
      } catch (final SniperException e) {
        log.info(
          "Liquidity pair for {}-{} does not exist yet",
          lpToken0.getSymbol(),
          lpToken1.getSymbol()
        );

        Thread.sleep(loopPauseInMillis);
      }
    }

    // define our conditions for swapping

    // conditions have been met, swap

    if (config.getMode() == Mode.SINGLE_SWAP_ALL_TOKEN_IN) {
      singleSwapAllInToken(
        inToken,
        outToken,
        inAmount,
        txManager.getFromAddress()
      );
    } else if (config.getMode() == Mode.MULTI_SWAP_TOKEN_OUT_TX_LIMIT) {
      multiSwapWithTokenOutTxLimit(
        inToken,
        outToken,
        inAmount,
        txManager.getFromAddress()
      );
    } else if (config.getMode() == Mode.MULTI_SWAP_TOKEN_IN_TX_LIMIT) {
      multiSwapWithTokenInTxLimit(
        inToken,
        outToken,
        inAmount,
        txManager.getFromAddress()
      );
    } else {
      log.error("No mode specified in application.properties.");
    }
  }

  private void approveToken(final Token token, final BigInteger approvalAmount)
    throws Exception {
    if (approvalAmount.compareTo(token.allowance(router.getAddress())) > 0) {
      log.info(
        "Approving {} to spend {} {}",
        router.getAddress(),
        converter.toHuman(approvalAmount, token.getDecimals()),
        token.getSymbol()
      );

      final var tx = token.approve(router.getAddress(), approvalAmount).send();

      log.info("Approval successful, tx: {}", tx.getTransactionHash());
    } else {
      log.info(
        "{} already approved to spend {} {}",
        router.getAddress(),
        converter.toHuman(approvalAmount, token.getDecimals()),
        token.getSymbol()
      );
    }
  }

  private void approveTokens(
    final Token inToken,
    final Token outToken,
    final BigInteger inAmount
  )
    throws Exception {
    approveToken(inToken, inAmount.multiply(BigInteger.TWO));
    approveToken(outToken, BigInteger.TWO.pow(255));
  }

  private void singleSwapAllInToken(
    final Token inToken,
    final Token outToken,
    final BigInteger inAmount,
    final String to
  ) {
    log.info(
      "Creating single tx to swap {} {} for {}",
      converter.toHuman(inAmount, inToken.getDecimals()),
      inToken.getSymbol(),
      outToken.getSymbol()
    );

    var txSent = false;

    while (!txSent) {
      try {
        final var tokenOutMinAndTx = router.swapExactTokensForTokens(
          inToken,
          outToken,
          inAmount,
          to
        );
        final var tokenOutMin = tokenOutMinAndTx.getLeft();

        log.info(
          "Attempting to swap {} {} for {} {}",
          converter.toHuman(inAmount, inToken.getDecimals()),
          inToken.getSymbol(),
          converter.toHuman(tokenOutMin, outToken.getDecimals()),
          outToken.getSymbol()
        );

        final var tx = tokenOutMinAndTx.getRight().send();

        log.info("Swap tx successful: {}", tx.getTransactionHash());

        txSent = true;
      } catch (final Exception e) {
        if (
          e.getCause() != null &&
          e.getCause().getMessage() != null &&
          e.getCause().getMessage().contains("INSUFFICIENT_LIQ")
        ) {
          log.info("Pool has insufficient liquidity.");
        } else {
          log.error("Swap tx failed.", e);
        }
      }
    }
  }

  private void multiSwapWithTokenOutTxLimit(
    final Token inToken,
    final Token outToken,
    final BigInteger inAmount,
    final String to
  ) {
    txManager.enableManualNonceIncrement(true);

    log.info(
      "Creating multi tx to swap {} {} for {}",
      converter.toHuman(inAmount, inToken.getDecimals()),
      inToken.getSymbol(),
      outToken.getSymbol()
    );

    final List<CompletableFuture<TransactionReceipt>> txs = new LinkedList<>();
    BigInteger inAmountLeft = inAmount;
    BigInteger tokenOutTxLimit = null;

    Pair<BigInteger, RemoteFunctionCall<TransactionReceipt>> tokenInMaxAndTx =
      null;

    while (tokenInMaxAndTx == null) {
      try {
        tokenOutTxLimit = outToken.getMaxTxAmount();
        // tokenOutTxLimit = converter.fromHuman(0.02, outToken.getDecimals());

        tokenInMaxAndTx =
          router.swapTokensForExactTokens(
            inToken,
            outToken,
            tokenOutTxLimit,
            to
          );
      } catch (final SniperException e) {
        if (
          e.getCause() != null &&
          e.getCause().getMessage() != null &&
          e.getCause().getMessage().contains("sub-underflow")
        ) {
          tokenOutTxLimit =
            new BigDecimal(tokenOutTxLimit)
              .multiply(BigDecimal.valueOf(0.5))
              .toBigInteger();

          log.info(
            "Underflow error, reducing max tx amount to {} {}",
            converter.toHuman(tokenOutTxLimit, outToken.getDecimals()),
            outToken.getSymbol()
          );
        } else if (
          e.getCause() != null &&
          e.getCause().getMessage() != null &&
          e.getCause().getMessage().contains("INSUFFICIENT_LIQ")
        ) {
          log.info("Pool has insufficient liquidity.");
        } else {
          log.error("Swap tx failed.", e);
        }
      }
    }

    log.info(
      "Max tx amount is {} {}",
      converter.toHuman(tokenOutTxLimit, outToken.getDecimals()),
      outToken.getSymbol()
    );

    final var tokenInMax = tokenInMaxAndTx.getLeft();
    final var tokenIn = tokenInMax.divide(BigInteger.TWO);

    while (inAmountLeft.compareTo(BigInteger.ZERO) > 0) {
      try {
        log.info(
          "Reducing token in max from {} to {} {}",
          converter.toHuman(tokenInMax, inToken.getDecimals()),
          converter.toHuman(tokenIn, inToken.getDecimals()),
          inToken.getSymbol()
        );

        if (inAmountLeft.compareTo(tokenIn) > 0) {
          // Normal swapTokensForExactTokens.

          log.info(
            "Attempting to swap {} {} for any {}",
            converter.toHuman(tokenIn, inToken.getDecimals()),
            inToken.getSymbol(),
            outToken.getSymbol()
          );

          txs.add(
            router
              .swapExactTokensForAnyTokens(inToken, outToken, tokenIn, to)
              .sendAsync()
          );
          inAmountLeft = inAmountLeft.subtract(tokenIn);
        } else {
          // Last will be swapExactTokensForTokens.

          final var tx = router.swapExactTokensForAnyTokens(
            inToken,
            outToken,
            inAmountLeft,
            to
          );

          log.info(
            "Attempting to swap {} {} for any {}",
            converter.toHuman(inAmountLeft, inToken.getDecimals()),
            inToken.getSymbol(),
            outToken.getSymbol()
          );

          txs.add(tx.sendAsync());
          inAmountLeft = BigInteger.ZERO;
        }
      } catch (final Exception e) {
        if (
          e.getCause() != null &&
          e.getCause().getMessage() != null &&
          e.getCause().getMessage().contains("INSUFFICIENT_LIQ")
        ) {
          log.info("Pool has insufficient liquidity.");
        } else {
          log.error("Swap tx failed.", e);
        }
      }
    }

    log.info("{} tx's sent, waiting for them to finish...", txs.size());

    final var txReceipts = new LinkedList<TransactionReceipt>();
    final var txFailures = new LinkedList<TransactionReceipt>();
    final var txExceptions = new LinkedList<Exception>();

    for (final var tx : txs) {
      try {
        final var txReceipt = tx.join();

        if (txReceipt.isStatusOK()) {
          txReceipts.add(txReceipt);
        } else {
          txFailures.add(txReceipt);
        }
      } catch (final CompletionException e) {
        txExceptions.add(e);
      }
    }

    log.info(
      "All transactions finised: {} successful, {} failures, {} exceptions",
      txReceipts.size(),
      txFailures.size(),
      txExceptions.size()
    );

    txReceipts.forEach(
      txReceipt ->
        log.info("Tx success. Hash: {}", txReceipt.getTransactionHash())
    );
    txFailures.forEach(
      txFailure ->
        log.info(
          "Tx failure. Hash {}; revert reason: {}, status: {}",
          txFailure.getTransactionHash(),
          txFailure.getRevertReason(),
          txFailure.getStatus()
        )
    );
    txExceptions.forEach(
      txException -> log.info("Tx exception. {}", txException)
    );

    txManager.enableManualNonceIncrement(false);
  }

  private void multiSwapWithTokenInTxLimit(
    final Token inToken,
    final Token outToken,
    final BigInteger inAmount,
    final String to
  )
    throws InterruptedException {
    txManager.enableManualNonceIncrement(true);

    log.info(
      "Creating multi tx to swap {} {} for {}",
      converter.toHuman(inAmount, inToken.getDecimals()),
      inToken.getSymbol(),
      outToken.getSymbol()
    );

    // Get the tx limit here.
    // final var tokenInTxLimit = inToken.getMaxTxAmount();
    final var tokenInTxLimit = converter.fromHuman(0.02, outToken.getDecimals());

    log.info(
      "Max tx amount: {} {}",
      converter.toHuman(tokenInTxLimit, inToken.getDecimals()),
      inToken.getSymbol()
    );

    final List<CompletableFuture<TransactionReceipt>> txs = new LinkedList<>();
    BigInteger inAmountLeft = inAmount;

    while (inAmountLeft.compareTo(BigInteger.ZERO) > 0) {
      var inAmountForTx = inAmountLeft.compareTo(tokenInTxLimit) > 0
        ? tokenInTxLimit
        : inAmountLeft;

      try {
        RemoteFunctionCall<TransactionReceipt> tx = null;

        while (tx == null) {
          try {
            tx =
              router.swapExactTokensForAnyTokens(
                inToken,
                outToken,
                inAmountForTx,
                to
              );
          } catch (final SniperException e) {
            if (
              e.getCause() != null &&
              e.getCause().getMessage() != null &&
              e.getCause().getMessage().contains("sub-underflow")
            ) {
              inAmountForTx =
                new BigDecimal(inAmountForTx)
                  .multiply(BigDecimal.valueOf(0.5))
                  .toBigInteger();

              log.info(
                "Underflow error, reducing max tx amount to {} {}",
                converter.toHuman(inAmountForTx, outToken.getDecimals()),
                outToken.getSymbol()
              );
            } else {
              throw e;
            }
          }
        }

        log.info(
          "Attempting to swap {} {} for any {}",
          converter.toHuman(inAmountForTx, inToken.getDecimals()),
          inToken.getSymbol(),
          outToken.getSymbol()
        );

        txs.add(tx.sendAsync());
        inAmountLeft = inAmountLeft.subtract(inAmountForTx);
      } catch (final Exception e) {
        if (
          e.getCause() != null &&
          e.getCause().getMessage() != null &&
          e.getCause().getMessage().contains("INSUFFICIENT_LIQ")
        ) {
          log.info("Pool has insufficient liquidity.");
        } else {
          log.error("Swap tx failed.", e);
        }
      }
    }

    log.info("{} tx's sent, waiting for them to finish...", txs.size());

    final var txReceipts = new LinkedList<TransactionReceipt>();
    final var txFailures = new LinkedList<TransactionReceipt>();
    final var txExceptions = new LinkedList<Exception>();

    for (final var tx : txs) {
      try {
        final var txReceipt = tx.join();

        if (txReceipt.isStatusOK()) {
          txReceipts.add(txReceipt);
        } else {
          txFailures.add(txReceipt);
        }
      } catch (final CompletionException e) {
        txExceptions.add(e);
      }
    }

    log.info(
      "All transactions finised: {} successful, {} failures, {} exceptions",
      txReceipts.size(),
      txFailures.size(),
      txExceptions.size()
    );

    txReceipts.forEach(
      txReceipt ->
        log.info("Tx success. Hash: {}", txReceipt.getTransactionHash())
    );
    txFailures.forEach(
      txFailure ->
        log.info(
          "Tx failure. Hash {}; revert reason: {}, status: {}",
          txFailure.getTransactionHash(),
          txFailure.getRevertReason(),
          txFailure.getStatus()
        )
    );
    txExceptions.forEach(
      txException -> log.info("Tx exception. {}", txException)
    );

    txManager.enableManualNonceIncrement(false);
  }
}
