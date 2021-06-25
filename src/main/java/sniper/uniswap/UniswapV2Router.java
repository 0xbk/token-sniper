package sniper.uniswap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.web3j.generated.contracts.IUniswapV2Router;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import sniper.EstimatingGasProvider;
import sniper.Factory;
import sniper.GasStation;
import sniper.Router;
import sniper.Token;
import sniper.generic.IERC20Token;

@Log4j2
public class UniswapV2Router implements Router {

  private final IUniswapV2Router router;
  private final Web3j web3j;
  private final Factory factory;
  private final Token weth;

  public UniswapV2Router(
    final String contractAddress,
    final Web3j web3j,
    final TransactionManager txManager,
    final GasStation gasStation
  )
    throws Exception {
    log.traceEntry(
      () -> contractAddress,
      () -> web3j,
      () -> txManager,
      () -> gasStation
    );

    this.router =
      IUniswapV2Router.load(
        contractAddress,
        web3j,
        txManager,
        new EstimatingGasProvider(contractAddress, web3j, txManager, gasStation)
      );
    this.web3j = web3j;
    this.factory =
      new UniswapV2Factory(
        this.router.factory().send(),
        web3j,
        txManager,
        gasStation
      );
    this.weth =
      new IERC20Token(this.router.WETH().send(), web3j, txManager, gasStation);

    log.info("Created router @ address {}", contractAddress);
    log.traceExit();
  }

  public String getContractAddress() {
    return router.getContractAddress();
  }

  public Factory factory() {
    return factory;
  }

  public Token weth() {
    return weth;
  }

  @SuppressWarnings("unchecked")
  public List<BigInteger> getAmountsOut(
    final BigInteger amountIn,
    List<Token> path
  )
    throws Exception {
    log.traceEntry(() -> amountIn, () -> path);

    final var pathContractAddresses = path
      .stream()
      .map(Token::getContractAddress)
      .collect(Collectors.toList());

    return log.traceExit(
      router.getAmountsOut(amountIn, pathContractAddresses).send()
    );
  }

  @SuppressWarnings("unchecked")
  public List<BigInteger> getAmountsIn(
    final BigInteger amountOut,
    List<Token> path
  )
    throws Exception {
    log.traceEntry(() -> amountOut, () -> path);

    final var pathContractAddresses = path
      .stream()
      .map(Token::getContractAddress)
      .collect(Collectors.toList());

    return log.traceExit(
      router.getAmountsIn(amountOut, pathContractAddresses).send()
    );
  }

  public RemoteFunctionCall<TransactionReceipt> swapExactTokensForTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final String to,
    final BigDecimal slippageInPercent
  )
    throws Exception {
    log.traceEntry(
      () -> tokenIn,
      () -> tokenOut,
      () -> amountIn,
      () -> to,
      () -> slippageInPercent
    );

    final var path = createSwapPath(tokenIn, tokenOut);
    var amountOutMin = getAmountsOut(amountIn, path).get(path.size() - 1);
    final var deadline = web3j
      .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
      .send()
      .getBlock()
      .getTimestamp()
      .add(BigInteger.valueOf(60));

    log.info(
      "Attempting to swap {} tokens for {} tokens by deadline {}",
      amountIn,
      amountOutMin,
      deadline
    );

    amountOutMin =
      amountOutMin.subtract(
        BigInteger.valueOf(
          (long) (
            amountOutMin.doubleValue() *
            (slippageInPercent.doubleValue() / 100.0)
          )
        )
      );

    return log.traceExit(
      router.swapExactTokensForTokens(
        amountIn,
        amountOutMin,
        path
          .stream()
          .map(Token::getContractAddress)
          .collect(Collectors.toList()),
        to,
        deadline
      )
    );
  }

  public RemoteFunctionCall<TransactionReceipt> swapTokensForExactTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountOut,
    final String to,
    final BigDecimal slippageInPercent
  )
    throws Exception {
    log.traceEntry(() -> tokenIn, () -> tokenOut, () -> amountOut, () -> to);

    final var path = createSwapPath(tokenIn, tokenOut);
    var amountInMax = getAmountsIn(amountOut, path).get(0);
    final var deadline = web3j
      .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
      .send()
      .getBlock()
      .getTimestamp()
      .add(BigInteger.valueOf(60));

    log.info(
      "Swapping {} tokens for {} tokens by deadline {}",
      amountInMax,
      amountOut,
      deadline
    );

    amountInMax =
      amountInMax.add(
        BigInteger.valueOf(
          (long) (
            amountInMax.doubleValue() *
            (slippageInPercent.doubleValue() / 100.0)
          )
        )
      );

    return log.traceExit(
      router.swapTokensForExactTokens(
        amountOut,
        amountInMax,
        path
          .stream()
          .map(Token::getContractAddress)
          .collect(Collectors.toList()),
        to,
        deadline
      )
    );
  }

  private List<Token> createSwapPath(
    final Token tokenIn,
    final Token tokenOut
  ) {
    List<Token> path;

    if (
      tokenIn.getContractAddress().equals(weth.getContractAddress()) ||
      tokenOut.getContractAddress().equals(weth.getContractAddress())
    ) {
      path = Arrays.asList(tokenIn, tokenOut);
    } else {
      path = Arrays.asList(tokenIn, weth(), tokenOut);
    }

    return path;
  }
}
