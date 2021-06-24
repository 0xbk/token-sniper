package sniper;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.springframework.stereotype.Component;

@Component
public class Converter {

  public BigInteger fromHuman(
    final double humanAmount,
    final BigInteger decimals
  ) {
    return BigDecimal
      .valueOf(humanAmount)
      .multiply(BigDecimal.TEN.pow(decimals.intValue()))
      .toBigInteger();
  }

  public BigDecimal toHuman(
    final BigInteger amount,
    final BigInteger decimals
  ) {
    return BigDecimal.valueOf(
      BigDecimal
        .valueOf(amount.doubleValue())
        .divide(BigDecimal.TEN.pow(decimals.intValue()))
        .doubleValue()
    );
  }
}
