package hudson.plugins.performance.util;

import java.text.DecimalFormat;

public class CalculationUtil {

  public static double roundTwoDecimals(DecimalFormat formatter, double d) {
    synchronized (formatter) {
      return Double.valueOf(formatter.format(d));
    }
  }

}
