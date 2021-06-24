package sniper;

import java.math.BigInteger;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface Token {
  String getContractAddress();
  RemoteFunctionCall<TransactionReceipt> approve(
    final String spender,
    final BigInteger amount
  );
  BigInteger decimals();
  String symbol();
}
