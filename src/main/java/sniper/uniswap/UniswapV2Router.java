package sniper.uniswap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.web3j.generated.contracts.IUniswapV2Router;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import sniper.Converter;
import sniper.Factory;
import sniper.GasStation;
import sniper.Router;
import sniper.SniperException;
import sniper.Token;
import sniper.TokenFactory;
import sniper.config.RouterConfig;
import sniper.config.SwapConfig;

@Log4j2
public class UniswapV2Router implements Router {

  private final IUniswapV2Router router;
  private final Web3j web3j;
  private final Factory factory;
  private final Token weth;
  private final Converter converter;
  private final SwapConfig swapConfig;

  public UniswapV2Router(
    final String contractAddress,
    final Web3j web3j,
    final TransactionManager txManager,
    final GasStation gasStation,
    final ContractGasProvider gasProvider,
    final TokenFactory token,
    final Converter converter,
    final SwapConfig swapConfig
  ) {
    log.traceEntry(
      () -> contractAddress,
      () -> web3j,
      () -> txManager,
      () -> gasStation,
      () -> gasProvider,
      () -> token,
      () -> converter,
      () -> swapConfig
    );

    this.router =
      IUniswapV2Router.load(contractAddress, web3j, txManager, gasProvider);
    this.web3j = web3j;

    try {
      this.factory =
        new UniswapV2Factory(
          this.router.factory().send(),
          web3j,
          txManager,
          gasStation,
          gasProvider,
          token
        );
      this.weth = token.from(this.router.WETH().send());
    } catch (final Exception e) {
      throw new SniperException("Failed to create the router.", e);
    }

    this.converter = converter;
    this.swapConfig = swapConfig;

    log.info("Created router @ address {}", contractAddress);
    log.traceExit();
  }

  public String getAddress() {
    return router.getContractAddress();
  }

  public Factory getFactory() {
    return factory;
  }

  public RouterConfig.Type getType() {
    return RouterConfig.Type.UNISWAPV2;
  }

  public Token getWeth() {
    return weth;
  }

  public BigInteger getAmountOut(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn
  ) {
    log.traceEntry(() -> tokenIn, () -> tokenOut, () -> amountIn);

    final var path = createSwapPath(tokenIn, tokenOut);
    final var pathContractAddresses = path
      .stream()
      .map(Token::getAddress)
      .collect(Collectors.toList());

    try {
      return log.traceExit(
        (BigInteger) router
          .getAmountsOut(amountIn, pathContractAddresses)
          .send()
          .get(path.size() - 1)
      );
    } catch (final Exception e) {
      throw new SniperException("Failed to get the amount out.", e);
    }
  }

  public BigInteger getAmountIn(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountOut
  ) {
    log.traceEntry(() -> tokenIn, () -> tokenOut, () -> amountOut);

    final var path = createSwapPath(tokenIn, tokenOut);
    final var pathContractAddresses = path
      .stream()
      .map(Token::getAddress)
      .collect(Collectors.toList());

    try {
      return log.traceExit(
        (BigInteger) router
          .getAmountsIn(amountOut, pathContractAddresses)
          .send()
          .get(0)
      );
    } catch (final Exception e) {
      throw new SniperException("Failed to get the amount in.", e);
    }
  }

  @FunctionalInterface
  private interface SwapTokens {
    RemoteFunctionCall<TransactionReceipt> doIt(
      final BigInteger amountIn,
      final BigInteger amountOutMin,
      final List<String> path,
      final String to,
      final BigInteger deadline
    );
  }

  public RemoteFunctionCall<TransactionReceipt> swapExactTokensForTokensCommon(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final BigInteger amountOutMin,
    final String to,
    final SwapTokens swapTokens
  ) {
    log.traceEntry(() -> tokenIn, () -> tokenOut, () -> amountIn, () -> to);

    final var path = createSwapPath(tokenIn, tokenOut);

    try {
      final var deadline = web3j
        .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
        .send()
        .getBlock()
        .getTimestamp()
        .add(BigInteger.valueOf(60));

      return log.traceExit(
        swapTokens.doIt(
          amountIn,
          amountOutMin,
          path.stream().map(Token::getAddress).collect(Collectors.toList()),
          to,
          deadline
        )
      );
    } catch (final Exception e) {
      throw new SniperException("Failed to setup a swap tx.", e);
    }
  }

  public RemoteFunctionCall<TransactionReceipt> swapExactTokensForTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final BigInteger amountOutMin,
    final String to
  ) {
    log.traceEntry(
      () -> tokenIn,
      () -> tokenOut,
      () -> amountIn,
      () -> amountOutMin,
      () -> to
    );
    return log.traceExit(
      swapExactTokensForTokensCommon(
        tokenIn,
        tokenOut,
        amountIn,
        amountOutMin,
        to,
        router::swapExactTokensForTokens
      )
    );
  }

  public RemoteFunctionCall<TransactionReceipt> swapExactTokensForTokensSupportingFeeOnTransferTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final BigInteger amountOutMin,
    final String to
  ) {
    log.traceEntry(
      () -> tokenIn,
      () -> tokenOut,
      () -> amountIn,
      () -> amountOutMin,
      () -> to
    );
    return log.traceExit(
      swapExactTokensForTokensCommon(
        tokenIn,
        tokenOut,
        amountIn,
        amountOutMin,
        to,
        router::swapExactTokensForTokensSupportingFeeOnTransferTokens
      )
    );
  }

  public Pair<BigInteger, RemoteFunctionCall<TransactionReceipt>> swapTokensForExactTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountInMax,
    final BigInteger amountOut,
    final String to
  ) {
    log.traceEntry(() -> tokenIn, () -> tokenOut, () -> amountOut, () -> to);

    final var path = createSwapPath(tokenIn, tokenOut);
    final var amountInMaxWithSlippage = amountInMax.add(
      new BigDecimal(amountInMax)
        .multiply(BigDecimal.valueOf(swapConfig.getSlippage()))
        .toBigInteger()
    );

    log.info(
      "Swap amount in max estimated to be {} {}; {} {} with {} slippage applied",
      converter.toHuman(amountInMax, tokenIn.getDecimals()),
      tokenIn.getSymbol(),
      converter.toHuman(amountInMaxWithSlippage, tokenIn.getDecimals()),
      tokenIn.getSymbol(),
      swapConfig.getSlippage()
    );

    try {
      final var deadline = web3j
        .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
        .send()
        .getBlock()
        .getTimestamp()
        .add(BigInteger.valueOf(60));

      return log.traceExit(
        Pair.of(
          amountInMaxWithSlippage,
          router.swapTokensForExactTokens(
            amountOut,
            amountInMaxWithSlippage,
            path.stream().map(Token::getAddress).collect(Collectors.toList()),
            to,
            deadline
          )
        )
      );
    } catch (final Exception e) {
      throw new SniperException("Failed to setup a swap tx.", e);
    }
  }

  private List<Token> createSwapPath(
    final Token tokenIn,
    final Token tokenOut
  ) {
    List<Token> path;

    if (
      tokenIn.getAddress().equals(weth.getAddress()) ||
      tokenOut.getAddress().equals(weth.getAddress())
    ) {
      path = Arrays.asList(tokenIn, tokenOut);
    } else {
      path = Arrays.asList(tokenIn, getWeth(), tokenOut);
    }

    return path;
  }
}
