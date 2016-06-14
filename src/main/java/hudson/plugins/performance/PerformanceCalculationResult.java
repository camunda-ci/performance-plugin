package hudson.plugins.performance;

import hudson.plugins.performance.report.UriReport;

/**
 * @author Christian Lipphardt
 */
public class PerformanceCalculationResult {

  private final String currentStaplerUri;
  private final String previousStaplerUri;

  private long currentAverage;
  private long previousAverage;
  private double relativeDiffAverage;
  private double relativeDiffAveragePercentage;

  private long currentMedian;
  private long previousMedian;
  private double relativeDiffMedian;
  private double relativeDiffMedianPercentage;

  private long current90Percentile;
  private long previous90Percentils;
  private double relativeDiff90Percentile;
  private double relativeDiff90PercentilePercentage;

  public PerformanceCalculationResult(UriReport current, UriReport previous) {
    this.currentStaplerUri = current.getStaplerUri();
    this.previousStaplerUri = previous.getStaplerUri();

    this.currentAverage = current.getAverage();
    this.previousAverage = previous.getAverage();
    this.currentMedian = current.getMedian();
    this.previousMedian = previous.getMedian();
    this.current90Percentile = current.get90Line();
    this.previous90Percentils = previous.get90Line();

    calculateResults();
  }

  public PerformanceCalculationResult(UriReport current, AggregatedUriReportCalculationResult aggregatedUriReportCalculationResult) {
    this.currentStaplerUri = current.getStaplerUri();
    this.previousStaplerUri = aggregatedUriReportCalculationResult.getUri();

    this.currentAverage = current.getAverage();
    this.previousAverage = aggregatedUriReportCalculationResult.getAverage();
    this.currentMedian = current.getMedian();
    this.previousMedian = aggregatedUriReportCalculationResult.getMedian();
    this.current90Percentile = current.get90Line();
    this.previous90Percentils = aggregatedUriReportCalculationResult.getPercentile();

    calculateResults();
  }

  protected void calculateResults() {
    getRelativeDiffAverage();
    getRelativeDiffAveragePercentage();
    getRelativeDiffMedian();
    getRelativeDiffMedianPercentage();
    getRelativeDiff90Percentile();
    getRelativeDiff90PercentilePercentage();
  }

  protected double asPercentage(double relativeDiff, long previousValue) {
    return Math.round(((relativeDiff * 100) / previousValue) * 100) / 100;
  }

  public double getRelativeDiffAverage() {
    relativeDiffAverage = currentAverage - previousAverage;
    return relativeDiffAverage;
  }

  public double getRelativeDiffAveragePercentage() {
    relativeDiffAveragePercentage = asPercentage(getRelativeDiffAverage(), previousAverage);
    return relativeDiffAveragePercentage;
  }

  public double getRelativeDiffMedian() {
    relativeDiffMedian = currentMedian - previousMedian;
    return relativeDiffMedian;
  }

  public double getRelativeDiffMedianPercentage() {
    relativeDiffMedianPercentage = asPercentage(getRelativeDiffMedian(), previousMedian);
    return relativeDiffMedianPercentage;
  }

  public double getRelativeDiff90Percentile() {
    relativeDiff90Percentile = current90Percentile - previous90Percentils;
    return relativeDiff90Percentile;
  }

  public double getRelativeDiff90PercentilePercentage() {
    relativeDiff90PercentilePercentage = asPercentage(getRelativeDiff90Percentile(), previous90Percentils);
    return relativeDiff90PercentilePercentage;
  }

  public long getCurrent90Percentile() {
    return current90Percentile;
  }

  public String getCurrentStaplerUri() {
    return currentStaplerUri;
  }

  public long getCurrentAverage() {
    return currentAverage;
  }

  public long getCurrentMedian() {
    return currentMedian;
  }

  public long getPrevious90Percentile() {
    return previous90Percentils;
  }

  public String getPreviousStaplerUri() {
    return previousStaplerUri;
  }

  public long getPreviousAverage() {
    return previousAverage;
  }

  public long getPreviousMedian() {
    return previousMedian;
  }

  @Override
  public String toString() {
    return "PerformanceCalculationResult{" +
        "currentStaplerUri='" + currentStaplerUri + '\'' +
        ", previousStaplerUri='" + previousStaplerUri + '\'' +
        ", currentAverage=" + currentAverage +
        ", previousAverage=" + previousAverage +
        ", relativeDiffAverage=" + relativeDiffAverage +
        ", relativeDiffAveragePercentage=" + relativeDiffAveragePercentage +
        ", currentMedian=" + currentMedian +
        ", previousMedian=" + previousMedian +
        ", relativeDiffMedian=" + relativeDiffMedian +
        ", relativeDiffMedianPercentage=" + relativeDiffMedianPercentage +
        ", current90Percentile=" + current90Percentile +
        ", previous90Percentils=" + previous90Percentils +
        ", relativeDiff90Percentile=" + relativeDiff90Percentile +
        ", relativeDiff90PercentilePercentage=" + relativeDiff90PercentilePercentage +
        '}';
  }
}
