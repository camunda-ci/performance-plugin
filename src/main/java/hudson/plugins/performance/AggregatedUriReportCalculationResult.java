package hudson.plugins.performance;

public class AggregatedUriReportCalculationResult implements Comparable<AggregatedUriReportCalculationResult> {

  private final String uri;
  private final long average;
  private final long median;
  private final long percentile;

  public AggregatedUriReportCalculationResult(String uri, long average, long median, long percentile) {
    this.uri = uri;
    this.average = average;
    this.median = median;
    this.percentile = percentile;
  }

  public long getPercentile() {
    return percentile;
  }

  public long getAverage() {
    return average;
  }

  public long getMedian() {
    return median;
  }

  public String getUri() {
    return uri;
  }

  @Override
  public int compareTo(AggregatedUriReportCalculationResult other) {
    if (other.getUri().equals(uri)) {
      return 0;
    } else {
      return 1;
    }
  }
}
