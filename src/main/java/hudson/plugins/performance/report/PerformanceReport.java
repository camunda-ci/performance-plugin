package hudson.plugins.performance.report;

import hudson.model.AbstractBuild;
import hudson.plugins.performance.Messages;
import hudson.plugins.performance.PerformanceBuildAction;
import hudson.plugins.performance.model.HttpPerformanceReportSample;
import hudson.plugins.performance.parser.PerformanceReportParser;
import hudson.plugins.performance.util.URIHelper;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;

import static hudson.plugins.performance.util.CalculationUtil.roundTwoDecimals;

/**
 * Represents a single performance report, which consists of multiple {@link UriReport}s for different URLs that was tested.
 * 
 * This object belongs under {@link PerformanceReportMap}.
 */
public class PerformanceReport extends AbstractHttpReport implements Comparable<PerformanceReport> {

  private transient PerformanceBuildAction buildAction;
  private String reportFileName = null;
  /**
   * {@link UriReport}s keyed by their {@link UriReport#getStaplerUri()}.
   */
  private final Map<String, UriReport> uriReportMap = new LinkedHashMap<String, UriReport>();
  private PerformanceReport lastBuildReport;
  /**
   * A lazy cache of all duration values of all HTTP samples in all UriReports, ordered by duration. 
   */
  private transient List<Long> durationsSortedBySize = null;
  /**
   * A lazy cache of all UriReports, reverse-ordered.
   */
  private transient List<UriReport> uriReportsOrdered = null;
  /**
   * The amount of http samples that are not successful.
   */
  private int nbError = 0;
  /**
   * The sum of summarizerErrors values from all samples;
   */
  private float summarizerErrors = 0;
  /**
   * The amount of samples in all uriReports combined.
   */
  private int size;
  /**
   * The duration of all samples combined, in milliseconds.
   */
  private long totalDuration = 0;
  /**
   * The size of all samples combined, in kilobytes.
   */
  private double totalSizeInKB = 0;
  /**
   * The longest duration from all samples, or Long.MIN_VALUE when no samples where processed.
   */
  private long max = Long.MIN_VALUE;
  /**
   * The shortest duration from all samples, or Long.MAX_VALUE when no samples where processed.
   */
  private long min = Long.MAX_VALUE;

  
  public void addSample(HttpPerformanceReportSample performanceSample) throws SAXException {
    String uri = performanceSample.getUri();
    if (uri == null) {
      buildAction.getConsoleWriter()
          .println("label cannot be empty, please ensure your jmx file specifies "
              + "name properly for each http sample: skipping sample");
      return;
    }
    String staplerUri = URIHelper.asStaplerURI(uri);
    synchronized (uriReportMap) {
      UriReport uriReport = uriReportMap.get(staplerUri);
      if (uriReport == null) {
        uriReport = new UriReport(staplerUri, uri);
        uriReportMap.put(staplerUri, uriReport);
      }
      uriReport.addHttpSample(performanceSample);

      // reset the lazy loaded caches.
      durationsSortedBySize = null;
      uriReportsOrdered = null;
    }
    
    if (!performanceSample.isSuccessful()) {
      nbError++;
    }
    summarizerErrors += performanceSample.getSummarizerErrors();
    size++;
    totalDuration += performanceSample.getDuration();
    totalSizeInKB += performanceSample.getSizeInKb();
    max = Math.max(performanceSample.getDuration(), max);
    min = Math.min(performanceSample.getDuration(), min);
  }

  public int compareTo(PerformanceReport jmReport) {
    if (this == jmReport) {
      return 0;
    }
    return getReportFileName().compareTo(jmReport.getReportFileName());
  }

  @Override
  public int countErrors() {
    return nbError;
  }

  @Override
  public double errorPercent() {
    if (ifSummarizerParserUsed(reportFileName)) {
      if (uriReportMap.size() == 0) return 0;
      return summarizerErrors / uriReportMap.size();
    } else {
      return size() == 0 ? 0 : ((double) countErrors()) / size() * 100;
    }
  }

  @Override
  public long getAverage() {
    if (size == 0) {
      return 0;
    }
    
    return totalDuration / size;
  }

  public double getAverageSizeInKb() {
    if (size == 0) {
      return 0;
    }
    return roundTwoDecimals(twoDForm, totalSizeInKB / size);
  }

  private long getDurationAt(double percentage) {
    if (percentage < 0 || percentage > 1) {
      throw new IllegalArgumentException("Argument 'percentage' must be a value between 0 and 1 (inclusive)");
    }

    if (size == 0) {
      return 0;
    }
    
    synchronized (uriReportMap) {
      if (durationsSortedBySize == null) {
        durationsSortedBySize = new ArrayList<Long>();
        for (UriReport currentReport : uriReportMap.values()) {
          durationsSortedBySize.addAll(currentReport.getDurations());
        }
        Collections.sort(durationsSortedBySize);
      }
      return durationsSortedBySize.get((int) (durationsSortedBySize.size() * percentage));
    }
  }

  @Override
  public long get90Line() {
    return getDurationAt(.9);
  }

  @Override
  public long getMedian() {
    return getDurationAt(.5);
  }

  public String getHttpCode() {
    return "";
  }

  public AbstractBuild<?, ?> getBuild() {
    return buildAction.getBuild();
  }

  PerformanceBuildAction getBuildAction() {
    return buildAction;
  }

  public String getDisplayName() {
    return Messages.Report_DisplayName();
  }

  public UriReport getDynamic(String token) throws IOException {
    return getUriReportMap().get(token);
  }

  public double getTotalTrafficInKb() {
    return roundTwoDecimals(twoDForm, totalSizeInKB);
  }

  @Override
  public long getMax() {
    return max;
  }

  @Override
  public long getMin() {
    return min;
  }

  public String getReportFileName() {
    return reportFileName;
  }

  public List<UriReport> getUriListOrdered() {
    synchronized (uriReportMap) {
      if (uriReportsOrdered == null) {
        uriReportsOrdered = new ArrayList<UriReport>(uriReportMap.values());
        Collections.sort(uriReportsOrdered, Collections.reverseOrder());
      }
      return uriReportsOrdered;
    }
  }

  public Map<String, UriReport> getUriReportMap() {
    return uriReportMap;
  }

  public void setBuildAction(PerformanceBuildAction buildAction) {
    this.buildAction = buildAction;
  }

  public void setReportFileName(String reportFileName) {
    this.reportFileName = reportFileName;
  }

  @Override
  public int size() {
    return size;
  }

  public void setLastBuildReport(PerformanceReport lastBuildReport) {
    Map<String, UriReport> lastBuildUriReportMap = lastBuildReport.getUriReportMap();
    for (Map.Entry<String, UriReport> item : uriReportMap.entrySet()) {
      UriReport lastBuildUri = lastBuildUriReportMap.get(item.getKey());
      if (lastBuildUri != null) {
        item.getValue().addLastBuildUriReport(lastBuildUri);
      }
    }
    this.lastBuildReport = lastBuildReport;
  }

  @Override
  public long getAverageDiff() {
    if (lastBuildReport == null) {
      return 0;
    }
    return getAverage() - lastBuildReport.getAverage();
  }

  @Override
  public long getMedianDiff() {
    if (lastBuildReport == null) {
      return 0;
    }
    return getMedian() - lastBuildReport.getMedian();
  }

  @Override
  public double getErrorPercentDiff() {
    if (lastBuildReport == null) {
      return 0;
    }
    return errorPercent() - lastBuildReport.errorPercent();
  }

  @Override
  public String getLastBuildHttpCodeIfChanged() {
    return "";
  }


  @Override
  public int getSizeDiff() {
    if (lastBuildReport == null) {
      return 0;
    }
    return size() - lastBuildReport.size();
  }

  /**
   * Check if the filename of the file being parsed is being parsed by a
   * summarized parser (JMeterSummarizer).
   * 
   * @param filename
   *          name of the file being parsed
   * @return boolean indicating usage of summarized parser
   */
  public boolean ifSummarizerParserUsed(String filename) {
    PerformanceReportParser parser = buildAction.getParserByDisplayName("JmeterSummarizer");
    if (parser != null) {
      String fileExt = parser.glob;
      String parts[] = fileExt.split("\\s*[;:,]+\\s*");
      for (String path : parts) {
        if (filename.endsWith(path.substring(5))) {
         return true;
        }
      }
    }
    parser = buildAction.getParserByDisplayName("Iago");
    return parser != null;
  }

}
