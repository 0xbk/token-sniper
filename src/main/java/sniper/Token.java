package sniper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.generated.contracts.IERC20;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

@Log4j2
public class Token {

  private final Web3Service web3j;
  private final TransManager transManager;
  private final GasStation gasStation;
  private final double gasLimitMultiplier;
  private final IERC20 ierc20;
  private final BigInteger decimals;
  private final String symbol;
  private String spender;
  private BigInteger amount;
  private ContractGasProvider gasProvider = new ContractGasProvider() {
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
      BigInteger gasLimit = BigInteger.ZERO;
      Function function = null;

      switch (contractFunc) {
        case IERC20.FUNC_APPROVE:
          {
            function =
              new Function(
                IERC20.FUNC_APPROVE,
                Arrays.<Type>asList(
                  new Address(160, spender),
                  new Uint256(amount)
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
        ierc20.getContractAddress(),
        encodedFunc
      );
      try {
        gasLimit =
          web3j.get().ethEstimateGas(transaction).send().getAmountUsed();

        log.info(
          "Estimated gas limit to call {} function to be {}",
          contractFunc,
          gasLimit
        );
      } catch (IOException e) {
        log.error(
          "Failed to estiamte gas limit to call {} function",
          contractFunc,
          e
        );
      }

      return gasLimit;
    }

    @Override
    public BigInteger getGasLimit() {
      return BigInteger.ZERO;
    }
  };

  public Token(
    final String contractAddress,
    final Web3Service web3j,
    final TransManager transManager,
    final GasStation gasStation,
    final double gasLimitMultiplier
  )
    throws Exception {
    this.web3j = web3j;
    this.transManager = transManager;
    this.gasStation = gasStation;
    this.gasLimitMultiplier = gasLimitMultiplier;
    this.ierc20 =
      IERC20.load(
        contractAddress,
        web3j.get(),
        transManager.get(),
        gasProvider
      );
    this.decimals = this.ierc20.decimals().send();
    this.symbol = this.ierc20.symbol().send();

    log.info(
      "Token {} @ address '{}' has {} decimals",
      symbol,
      contractAddress,
      decimals
    );
  }

  public IERC20 getContract() {
    return ierc20;
  }

  void approve(final String spender, final BigInteger amount) throws Exception {
    this.spender = spender;
    this.amount = amount;

    log.info("Approving {} amount for spender '{}'", amount, spender);
    final var transaction = ierc20.approve(spender, amount).send();

    if (!transaction.isStatusOK()) {
      throw new Exception(
        String.format(
          "Failed to approve token: %s",
          transaction.getRevertReason()
        )
      );
    }

    log.info("Tx has '{}'", transaction.getTransactionHash());
  }

  public BigInteger decimals() {
    return decimals;
  }

  public String symbol() {
    return symbol;
  }
}
