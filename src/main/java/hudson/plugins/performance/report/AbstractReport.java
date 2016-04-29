package hudson.plugins.performance.report;

import hudson.model.ModelObject;
import org.kohsuke.stapler.Stapler;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Abstract class for classes with size, error, mean, average, 90 line, min and max attributes
 */
public abstract class AbstractReport implements Serializable, ModelObject {

  private static final long serialVersionUID = 1L;

  protected final DecimalFormat percentFormat;
  protected final DecimalFormat dataFormat; // three decimals
  protected final DecimalFormat twoDForm; // two decimals

  public AbstractReport() {
    final Locale useThisLocale = ( Stapler.getCurrentRequest() != null ) ? Stapler.getCurrentRequest().getLocale() : Locale.getDefault();

    percentFormat = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance( useThisLocale ));
    dataFormat = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance( useThisLocale ));
    twoDForm = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance( useThisLocale ));
  }

  abstract public int countErrors();

  abstract public double errorPercent();

  abstract public long getAverage();

  abstract public long getMedian();

  abstract public long get90Line();

  abstract public long getMax();

  abstract public long getMin();

  abstract public int size();

  abstract public long getAverageDiff();

  abstract public long getMedianDiff();

  abstract public double getErrorPercentDiff();

  abstract public int getSizeDiff();

  public String errorPercentFormated() {
    Stapler.getCurrentRequest().getLocale();
    synchronized (percentFormat) {
      return percentFormat.format(errorPercent());
    }
  }

  public String getAverageFormated() {
    synchronized (dataFormat) {
      return dataFormat.format(getAverage());
    }
  }

  public String getMeanFormated() {
    synchronized (dataFormat) {
      return dataFormat.format(getMedian());
    }
  }

  public String get90LineFormated() {
    synchronized (dataFormat) {
      return dataFormat.format(get90Line());
    }
  }

  public String getMaxFormated() {
    synchronized (dataFormat) {
      return dataFormat.format(getMax());
    }
  }

}
