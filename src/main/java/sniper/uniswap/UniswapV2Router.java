package sniper.uniswap;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.generated.contracts.IERC20;
import org.web3j.generated.contracts.IUniswapV2Factory;
import org.web3j.generated.contracts.IUniswapV2Router;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import sniper.GasStation;
import sniper.Router;
import sniper.Token;
import sniper.TransManager;
import sniper.Web3Service;

public class UniswapV2Router implements Router {

  private final IUniswapV2Router router;
  private final Web3Service web3;
  private final TransManager transManager;
  private final GasStation gasStation;
  private final Logger log = LogManager.getLogger();
  private BigInteger amountIn;
  private BigInteger amountOutMin;
  private BigInteger amountInMax;
  private BigInteger amountOut;
  private List<String> path;
  private String to;
  private BigInteger deadline;
  private final ContractGasProvider gasProvider = new ContractGasProvider() {
    @Override
    public BigInteger getGasPrice(String contractFunc) {
      return gasStation.getGasPrice();
    }

    @Override
    public BigInteger getGasPrice() {
      return gasStation.getGasPrice();
    }

    @Override
    public BigInteger getGasLimit(String contractFunc) {
      BigInteger gasPrice = BigInteger.ZERO;
      Function function = null;

      switch (contractFunc) {
        case IUniswapV2Router.FUNC_SWAPEXACTTOKENSFORTOKENS:
          {
            function =
              new Function(
                IUniswapV2Router.FUNC_SWAPEXACTTOKENSFORTOKENS,
                Arrays.<Type>asList(
                  new org.web3j.abi.datatypes.generated.Uint256(amountIn),
                  new org.web3j.abi.datatypes.generated.Uint256(amountOutMin),
                  new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                    org.web3j.abi.datatypes.Address.class,
                    org.web3j.abi.Utils.typeMap(
                      path,
                      org.web3j.abi.datatypes.Address.class
                    )
                  ),
                  new org.web3j.abi.datatypes.Address(160, to),
                  new org.web3j.abi.datatypes.generated.Uint256(deadline)
                ),
                Collections.<TypeReference<?>>emptyList()
              );
            break;
          }
        case IUniswapV2Router.FUNC_SWAPTOKENSFOREXACTTOKENS:
          {
            function =
              new Function(
                IUniswapV2Router.FUNC_SWAPTOKENSFOREXACTTOKENS,
                Arrays.<Type>asList(
                  new org.web3j.abi.datatypes.generated.Uint256(amountOut),
                  new org.web3j.abi.datatypes.generated.Uint256(amountInMax),
                  new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                    org.web3j.abi.datatypes.Address.class,
                    org.web3j.abi.Utils.typeMap(
                      path,
                      org.web3j.abi.datatypes.Address.class
                    )
                  ),
                  new org.web3j.abi.datatypes.Address(160, to),
                  new org.web3j.abi.datatypes.generated.Uint256(deadline)
                ),
                Collections.<TypeReference<?>>emptyList()
              );
            break;
          }
      }

      final var encodedFunc = FunctionEncoder.encode(function);
      final var transaction = Transaction.createFunctionCallTransaction(
        transManager.get().getFromAddress(),
        BigInteger.ZERO,
        BigInteger.ZERO,
        BigInteger.ZERO,
        router.getContractAddress(),
        encodedFunc
      );
      try {
        gasPrice =
          web3.get().ethEstimateGas(transaction).send().getAmountUsed();

        log.info(
          "Estimated gas limit to call {} function to be {}",
          contractFunc,
          gasPrice
        );
      } catch (IOException e) {
        log.error(
          "Failed to estimate gas limit to call {} function",
          contractFunc,
          e
        );
      }

      return gasPrice;
    }

    @Override
    public BigInteger getGasLimit() {
      return BigInteger.ZERO;
    }
  };

  public UniswapV2Router(
    final String contractAddress,
    final Web3Service web3,
    final TransManager transManager,
    final GasStation gasStation
  ) {
    this.router =
      IUniswapV2Router.load(
        contractAddress,
        web3.get(),
        transManager.get(),
        gasProvider
      );
    this.transManager = transManager;
    this.web3 = web3;
    this.gasStation = gasStation;
  }

  public IUniswapV2Router getContract() {
    return router;
  }

  public void swapExactTokensForTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountIn,
    final String to
  )
    throws Exception {
    this.amountIn = amountIn;
    this.path =
      Arrays.asList(
        tokenIn.getContract().getContractAddress(),
        tokenOut.getContract().getContractAddress()
      );
    this.to = to;
    this.amountOutMin =
      (BigInteger) router
        .getAmountsOut(amountIn, path)
        .send()
        .get(path.size() - 1);
    this.deadline =
      web3
        .get()
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

    // final BigInteger gasLimit = gasProvider.getGasLimit(
    //   IUniswapV2Router.FUNC_SWAPEXACTTOKENSFORTOKENS
    // );
    // final BigInteger gasPrice = gasProvider.getGasPrice(
    //   IUniswapV2Router.FUNC_SWAPEXACTTOKENSFORTOKENS
    // );
    // final BigInteger gasTotal = gasLimit.multiply(gasPrice);

    // log.info("Gas limit {}", gasLimit);
    // log.info("Gas price {}", gasPrice);
    // log.info("Gas total {}", gasTotal);

    final var transaction = router
      .swapExactTokensForTokens(amountIn, amountOutMin, path, to, deadline)
      .send();

    if (!transaction.isStatusOK()) {
      throw new Exception(
        String.format(
          "Failed to swap tokens: %s",
          transaction.getRevertReason()
        )
      );
    }

    log.info("Tx has '{}'", transaction.getTransactionHash());
  }

  public void swapTokensForExactTokens(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountOut,
    final String to
  )
    throws Exception {
    this.amountOut = amountOut;
    this.path =
      Arrays.asList(
        tokenIn.getContract().getContractAddress(),
        tokenOut.getContract().getContractAddress()
      );
    this.to = to;
    this.amountInMax =
      (BigInteger) router.getAmountsIn(amountOut, path).send().get(0);
    this.deadline =
      web3
        .get()
        .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
        .send()
        .getBlock()
        .getTimestamp()
        .add(BigInteger.valueOf(60));

    log.info(
      "Attempting to swap {} tokens for {} tokens by deadline {}",
      amountInMax,
      amountOut,
      deadline
    );

    // final BigInteger gasLimit = gasProvider.getGasLimit(
    //   IUniswapV2Router.FUNC_SWAPTOKENSFOREXACTTOKENS
    // );
    // final BigInteger gasPrice = gasProvider.getGasPrice(
    //   IUniswapV2Router.FUNC_SWAPTOKENSFOREXACTTOKENS
    // );
    // final BigInteger gasTotal = gasLimit.multiply(gasPrice);

    // log.info("Gas limit {}", gasLimit);
    // log.info("Gas price {}", gasPrice);
    // log.info("Gas total {}", gasTotal);

    final var transaction = router
      .swapTokensForExactTokens(amountOut, amountInMax, path, to, deadline)
      .send();

    if (!transaction.isStatusOK()) {
      throw new Exception(
        String.format(
          "Failed to swap tokens: %s",
          transaction.getRevertReason()
        )
      );
    }

    log.info("Tx is '{}'", transaction.getTransactionHash());
  }

  public CompletableFuture<TransactionReceipt> swapTokensForExactTokensAsync(
    final Token tokenIn,
    final Token tokenOut,
    final BigInteger amountOut,
    final String to
  )
    throws Exception {
    this.amountOut = amountOut;
    this.path =
      Arrays.asList(
        tokenIn.getContract().getContractAddress(),
        tokenOut.getContract().getContractAddress()
      );
    this.to = to;
    this.amountInMax =
      (BigInteger) router.getAmountsIn(amountOut, path).send().get(0);
    this.deadline =
      web3
        .get()
        .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
        .send()
        .getBlock()
        .getTimestamp()
        .add(BigInteger.valueOf(60));

    log.info(
      "Attempting to swap {} tokens for {} tokens by deadline {}",
      amountInMax,
      amountOut,
      deadline
    );

    // final BigInteger gasLimit = gasProvider.getGasLimit(
    //   IUniswapV2Router.FUNC_SWAPTOKENSFOREXACTTOKENS
    // );
    // final BigInteger gasPrice = gasProvider.getGasPrice(
    //   IUniswapV2Router.FUNC_SWAPTOKENSFOREXACTTOKENS
    // );
    // final BigInteger gasTotal = gasLimit.multiply(gasPrice);

    // log.info("Gas limit {}", gasLimit);
    // log.info("Gas price {}", gasPrice);
    // log.info("Gas total {}", gasTotal);

    return router
      .swapTokensForExactTokens(amountOut, amountInMax, path, to, deadline)
      .sendAsync();
  }
}
