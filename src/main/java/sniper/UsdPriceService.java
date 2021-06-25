package sniper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import sniper.generic.IERC20Token;

@Component
@Log4j2
public class UsdPriceService {

  @Autowired
  private Router router;

  private final Token usd1;

  public UsdPriceService(
    @Value(
      "${token.1dollarpegged.contractAddress}"
    ) final String contractAddress,
    @Autowired final Web3j web3j,
    @Autowired TransactionManager txManager,
    @Autowired GasStation gasStation
  )
    throws Exception {
    this.usd1 = new IERC20Token(contractAddress, web3j, txManager, gasStation);
  }

  public BigDecimal get(final Token token) throws Exception {
    log.traceEntry(() -> token);

    if (usd1.getContractAddress().equals(token.getContractAddress())) {
      return BigDecimal.ONE;
    }

    final BigInteger amountOut = router
      .getAmountsOut(
        BigInteger.ONE.multiply(
          BigInteger.TEN.pow(token.decimals().intValue())
        ),
        Arrays.asList(token, usd1)
      )
      .get(1);

    return log.traceExit(
      BigDecimal.valueOf(
        amountOut.doubleValue() / Math.pow(10.0, usd1.decimals().doubleValue())
      )
    );
  }

  public BigDecimal getFor(final Token token, final BigDecimal amount)
    throws Exception {
    log.traceEntry(() -> token, () -> amount);

    if (usd1.getContractAddress().equals(token.getContractAddress())) {
      return BigDecimal.ONE.multiply(amount);
    }

    final BigInteger amountOut = router
      .getAmountsOut(
        BigInteger.ONE.multiply(
          BigInteger.TEN.pow(token.decimals().intValue())
        ),
        Arrays.asList(token, usd1)
      )
      .get(1);

    return log.traceExit(
      BigDecimal
        .valueOf(
          amountOut.doubleValue() /
          Math.pow(10.0, usd1.decimals().doubleValue())
        )
        .multiply(amount)
    );
  }
}
