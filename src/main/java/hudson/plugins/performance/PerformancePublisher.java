package hudson.plugins.performance;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.performance.parser.JMeterParser;
import hudson.plugins.performance.parser.PerformanceReportParser;
import hudson.plugins.performance.parser.PerformanceReportParserDescriptor;
import hudson.plugins.performance.report.PerformanceReport;
import hudson.plugins.performance.report.PerformanceReportUtil;
import hudson.plugins.performance.report.UriReport;
import hudson.plugins.performance.util.AssertHelper;
import hudson.plugins.performance.util.DashboardFile;
import hudson.plugins.performance.util.IoUtil;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.*;
import java.util.*;

public class PerformancePublisher extends Recorder {

  public static final double THRESHOLD_TOLERANCE = 0.00000001;

  public static final String MODE_ART = "ART";
  public static final String MODE_MRT = "MRT";
  public static final String MODE_PRT = "PRT";

  private boolean thresholdMode = false; // false means 'Error Threshold', true: 'Relative Threshold'

  // start Error Threshold only properties
  private int errorFailedThreshold = 0;
  private int errorUnstableThreshold = 0;
  private String errorUnstableResponseTimeThreshold = "";
  // end Error Threshold only properties

  // start Relative Threshold only properties
  private double relativeFailedThresholdPositive = 0;
  private double relativeFailedThresholdNegative = 0;
  private double relativeUnstableThresholdPositive = 0;
  private double relativeUnstableThresholdNegative = 0;
  // end Relative Threshold only properties

  private int minimumThresholdTolerance = 0;
  // last successful build
  public static final String BUILD_MODE_LSB = "LSB";
  // last recent build
  public static final String BUILD_MODE_LRB = "LRB";
  // specific build(s)
  public static final String BUILD_MODE_SB = "SB";
  // last N builds
  public static final String BUILD_MODE_LNB = "LNB";
  private String compareWithBuildMode = BUILD_MODE_LSB;
  private String compareWithBuildNumbers = null;

  private String comparisonType = MODE_ART;

  private boolean modePerformancePerTestCase = false;
  private boolean modeThroughput;
  /**
   * @deprecated as of 1.3. for compatibility
   */
  private transient String filename;
  /**
   * Configured report parsers.
   */
  private List<PerformanceReportParser> parsers;

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public static final String MEDIAN_RESPONSE_TIME = "Median Response Time";
    public static final String AVERAGE_RESPONSE_TIME = "Average Response Time";
    public static final String PERCENTILE_RESPONSE_TIME = "Percentile Response Time";

    public static final String LAST_SUCCESSFUL_BUILD = "Last Successful Build";
    public static final String LAST_RECENT_BUILD = "Last Recent Build";
    public static final String LAST_N_BUILDS = "Last N Build(s)";
    public static final String SPECIFIC_BUILDS = "Specific Build(s)";

    @Override
    public String getDisplayName() {
      return Messages.Publisher_DisplayName();
    }

    @Override
    public String getHelpFile() {
      return "/plugin/performance/help.html";
    }

    public List<PerformanceReportParserDescriptor> getParserDescriptors() {
      return PerformanceReportParserDescriptor.all();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    /**
     * Populate the configType dynamically based on the user selection from the previous time.
     *
     * @return the name of the option selected in the previous run
     */
    public ListBoxModel doFillComparisonTypeItems(@QueryParameter String comparisonType) {
      ListBoxModel items = new ListBoxModel();

      if (comparisonType.equalsIgnoreCase(MODE_ART)) {
        items.add(AVERAGE_RESPONSE_TIME, MODE_ART);
        items.add(MEDIAN_RESPONSE_TIME, MODE_MRT);
        items.add(PERCENTILE_RESPONSE_TIME, MODE_PRT);
      } else if (comparisonType.equalsIgnoreCase(MODE_MRT)) {
        items.add(MEDIAN_RESPONSE_TIME, MODE_MRT);
        items.add(PERCENTILE_RESPONSE_TIME, MODE_PRT);
        items.add(AVERAGE_RESPONSE_TIME, MODE_ART);
      } else if (comparisonType.equalsIgnoreCase(MODE_PRT)) {
        items.add(PERCENTILE_RESPONSE_TIME, MODE_PRT);
        items.add(AVERAGE_RESPONSE_TIME, MODE_ART);
        items.add(MEDIAN_RESPONSE_TIME, MODE_MRT);
      } else {
        items.add(AVERAGE_RESPONSE_TIME, MODE_ART);
        items.add(MEDIAN_RESPONSE_TIME, MODE_MRT);
        items.add(PERCENTILE_RESPONSE_TIME, MODE_PRT);
      }

      return items;
    }

    /**
     * Populate the comparisonBuildMode dynamically based on the user selection from the previous time.
     *
     * @return the name of the option selected in the previous run
     */
    public ListBoxModel doFillCompareWithBuildModeItems(@QueryParameter String compareWithBuildMode) {
      ListBoxModel items = new ListBoxModel();

      if (compareWithBuildMode.equalsIgnoreCase(BUILD_MODE_LSB)) {
        items.add(LAST_SUCCESSFUL_BUILD, BUILD_MODE_LSB);
        items.add(LAST_RECENT_BUILD, BUILD_MODE_LRB);
        items.add(LAST_N_BUILDS, BUILD_MODE_LNB);
        items.add(SPECIFIC_BUILDS, BUILD_MODE_SB);
      } else if (compareWithBuildMode.equalsIgnoreCase(BUILD_MODE_LRB)) {
        items.add(LAST_RECENT_BUILD, BUILD_MODE_LRB);
        items.add(LAST_SUCCESSFUL_BUILD, BUILD_MODE_LSB);
        items.add(LAST_N_BUILDS, BUILD_MODE_LNB);
        items.add(SPECIFIC_BUILDS, BUILD_MODE_SB);
      } else if (compareWithBuildMode.equalsIgnoreCase(BUILD_MODE_LNB)) {
        items.add(LAST_N_BUILDS, BUILD_MODE_LNB);
        items.add(LAST_SUCCESSFUL_BUILD, BUILD_MODE_LSB);
        items.add(LAST_RECENT_BUILD, BUILD_MODE_LRB);
        items.add(SPECIFIC_BUILDS, BUILD_MODE_SB);
      } else if (compareWithBuildMode.equalsIgnoreCase(BUILD_MODE_SB)) {
        items.add(SPECIFIC_BUILDS, BUILD_MODE_SB);
        items.add(LAST_SUCCESSFUL_BUILD, BUILD_MODE_LSB);
        items.add(LAST_RECENT_BUILD, BUILD_MODE_LRB);
        items.add(LAST_N_BUILDS, BUILD_MODE_LNB);
      } else {
        items.add(LAST_SUCCESSFUL_BUILD, BUILD_MODE_LSB);
        items.add(LAST_RECENT_BUILD, BUILD_MODE_LRB);
        items.add(LAST_N_BUILDS, BUILD_MODE_LNB);
        items.add(SPECIFIC_BUILDS, BUILD_MODE_SB);
      }

      return items;
    }
  }

  @DataBoundConstructor
  public PerformancePublisher(int errorFailedThreshold,
                              int errorUnstableThreshold,
                              String errorUnstableResponseTimeThreshold,
                              double relativeFailedThresholdPositive,
                              double relativeFailedThresholdNegative,
                              double relativeUnstableThresholdPositive,
                              double relativeUnstableThresholdNegative,
                              String compareWithBuildMode,
                              String compareWithBuildNumbers,
                              int minimumThresholdTolerance,
                              boolean modePerformancePerTestCase,
                              String comparisonType,
                              boolean thresholdMode,
                              List<? extends PerformanceReportParser> parsers,
                              boolean modeThroughput) {

    this.errorFailedThreshold = errorFailedThreshold;
    this.errorUnstableThreshold = errorUnstableThreshold;
    this.errorUnstableResponseTimeThreshold = errorUnstableResponseTimeThreshold;

    this.relativeFailedThresholdPositive = relativeFailedThresholdPositive;
    this.relativeFailedThresholdNegative = relativeFailedThresholdNegative;
    this.relativeUnstableThresholdPositive = relativeUnstableThresholdPositive;
    this.relativeUnstableThresholdNegative = relativeUnstableThresholdNegative;

    this.compareWithBuildMode = compareWithBuildMode;
    this.compareWithBuildNumbers = compareWithBuildNumbers;

    this.minimumThresholdTolerance = minimumThresholdTolerance;

    this.comparisonType = comparisonType;
    this.thresholdMode = thresholdMode;

    if (parsers == null) {
      parsers = Collections.emptyList();
    }
    this.parsers = new ArrayList<PerformanceReportParser>(parsers);
    this.modePerformancePerTestCase = modePerformancePerTestCase;
    this.modeThroughput = modeThroughput;
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return new PerformanceProjectAction(project);
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  public List<PerformanceReportParser> getParsers() {
    return parsers;
  }


  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

    PrintStream logger = listener.getLogger();
    Result result = Result.SUCCESS;
    EnvVars env = build.getEnvironment(listener);

    //For absolute error/unstable threshold..
    if (!thresholdMode) {
      if (handleAbsoluteThreshold(build, listener, logger, result, env)) {
        return true;
      }
    } else {
      if (handleRelativeThreshold(build, listener, logger, env)) {
        return true;
      }
    }
    return true;
  }

  /**
   * For relative comparisons between builds.
   *
   * @param build
   * @param listener
   * @param logger
   * @param env
   * @return
   */
  protected boolean handleRelativeThreshold(AbstractBuild<?, ?> build, BuildListener listener, PrintStream logger, EnvVars env) {
    try {
      logger.print("\nPerformance: \n\n");
      logThresholdSettingsForResult(logger, Result.UNSTABLE, relativeUnstableThresholdNegative, relativeUnstableThresholdPositive);
      logThresholdSettingsForResult(logger, Result.FAILURE, relativeFailedThresholdNegative, relativeFailedThresholdPositive);


      // do calculations for all available current PerformanceReports
      // redo calculations or load from cache the PerformanceReports for the old N builds or specific build
      // - N builds : calculate average for all builds
      // - specific build | last successful build: usual calculations
      // compare the results of the calculations for the current and old performance reports
      // set build results based on calculations
      // log which test caused the build to fail or becoming unstable
      // maybe save the calculations so we do not have to recalculate it

      String name = "";
      List<PerformanceCalculationResult> performanceCalculationResults = new ArrayList<PerformanceCalculationResult>();
      Map<String, PerformanceReport> currentPerformanceReportsByFileName = new HashMap<String, PerformanceReport>();

      // add the report to the build object.
      PerformanceBuildAction performanceBuildAction = new PerformanceBuildAction(build, logger, parsers);
      build.addAction(performanceBuildAction);

      logger.print("\n\n");

      for (PerformanceReportParser parser : parsers) {
        String glob = env.expand(parser.glob);
        name = glob;
        List<FilePath> files = PerformanceReportUtil.locatePerformanceReports(build.getWorkspace(), glob);

        if (failBuildIfNoFilesArePresent(build, logger, parser, glob, files)) {
          // quit processing because no files found in current build.
          return true;
        }

        List<File> localReports = IoUtil.copyReportsToMaster(build, logger, files, parser.getDescriptor().getDisplayName());
        Collection<PerformanceReport> parsedReports = parser.parse(build, localReports, listener);

        for (PerformanceReport performanceReport : parsedReports) {
          performanceReport.setBuildAction(performanceBuildAction);
          currentPerformanceReportsByFileName.put(performanceReport.getReportFileName(), performanceReport);
        }
      }

      // getting previous build/nth previous build...
      List<AbstractBuild<?, ?>> previousBuilds = getComparisonBuilds(build, listener);
      List<Map<String, PerformanceReport>> previousPerformanceReportsByBuild = new ArrayList<Map<String, PerformanceReport>>();

      for (AbstractBuild<?, ?> previousBuild : previousBuilds) {
        Map<String, PerformanceReport> performanceReportsByFileName = getPerformanceReportsByFileNameForBuild(listener, logger, previousBuild);
        previousPerformanceReportsByBuild.add(performanceReportsByFileName);
      }

      List<AggregatedUriReportCalculationResult> aggregatedUriReportCalculationResults = calculateAggregatedPerformanceResultsForBuilds(previousPerformanceReportsByBuild);
      // compare current build with aggregated results from previous builds
      performanceCalculationResults = calculateDifferenceBetweenCurrentAndPreviousBuilds(currentPerformanceReportsByFileName, aggregatedUriReportCalculationResults);

      logPerformanceCalculationResults(comparisonType, compareWithBuildMode, compareWithBuildNumbers, build, performanceCalculationResults, logger);
      setBuildOutcomeBasedOnCalculations(build, performanceCalculationResults, logger);

      DashboardFile.writeAsXmlFile(relativeFailedThresholdNegative, relativeFailedThresholdPositive, relativeUnstableThresholdNegative, relativeUnstableThresholdPositive, build, name, performanceCalculationResults);

    } catch (Exception e) {
      throw new RuntimeException("Error while calculating performance results.", e);
    }
    return false;
  }

  protected void logPerformanceCalculationResults(String configType, String buildComparisonMode, String buildNumbers, AbstractBuild<?, ?> build, List<PerformanceCalculationResult> performanceCalculationResults, PrintStream logger) {
    if (buildComparisonMode.equalsIgnoreCase(BUILD_MODE_LSB)) {
      logger.print("\nComparison of results of last succesful build with " + build.getNumber() + " using mode: ");
    } else if (buildComparisonMode.equalsIgnoreCase(BUILD_MODE_LRB)) {
      logger.print("\nComparison of results of last recent build with " + build.getNumber() + " using mode: ");
    } else if (buildComparisonMode.equalsIgnoreCase(BUILD_MODE_LNB)) {
      logger.print("\nComparison of aggregated results from last " + buildNumbers + " builds with " + build.getNumber() + " using mode: ");
    } else if (buildComparisonMode.equalsIgnoreCase(BUILD_MODE_SB)) {
      logger.print("\nComparison of aggregated results from build(s) '" + compareWithBuildNumbers + "' with " + build.getNumber() + " using mode: ");
    } else {
      throw new IllegalArgumentException("BuildComparisonMode '" + buildComparisonMode + "' is not supported.");
    }

    if (configType.equalsIgnoreCase(MODE_ART)) {
      logger.print("Average response time\n");
      logger.println("====================================================================================================================================");
      logger.format("| %-52s | %-16s | %-16s | %-16s | %-16s |%n", "URI", "Previous Average", "Current Average", "Difference", "Percentage");
      logger.println("====================================================================================================================================");
      for (PerformanceCalculationResult result : performanceCalculationResults) {
        logger.format("| %-52s | %16d | %16d | %16.0f | %15.0f%% |%n",
            result.getCurrentStaplerUri(), result.getPreviousAverage(), result.getCurrentAverage(),
            result.getRelativeDiffAverage(), result.getRelativeDiffAveragePercentage());
      }
      logger.println("====================================================================================================================================");
    } else if (configType.equalsIgnoreCase(MODE_MRT)) {
      logger.print("Median response time\n");
      logger.println("====================================================================================================================================");
      logger.format("| %-52s | %-16s | %-16s | %-16s | %-16s |%n", "URI", "Previous Median", "Current Median", "Difference", "Percentage");
      logger.println("====================================================================================================================================");
      for (PerformanceCalculationResult result : performanceCalculationResults) {
        logger.format("| %-52s | %16d | %16d | %16.0f | %15.0f%% |%n",
            result.getCurrentStaplerUri(), result.getPreviousMedian(), result.getCurrentMedian(),
            result.getRelativeDiffMedian(), result.getRelativeDiffMedianPercentage());
      }
      logger.println("====================================================================================================================================");
    } else if (configType.equalsIgnoreCase(MODE_PRT)) {
      logger.print("90 Percentile response time\n");
      logger.println("====================================================================================================================================");
      logger.format("| %-52s | %-16s | %-16s | %-16s | %-16s |%n", "URI", "Prev. Percentile", "Cur. Percentile", "Difference", "Percentage");
      logger.println("====================================================================================================================================");
      for (PerformanceCalculationResult result : performanceCalculationResults) {
        logger.format("| %-52s | %16d | %16d | %16.0f | %15.0f%% |%n",
            result.getCurrentStaplerUri(), result.getPrevious90Percentile(), result.getCurrent90Percentile(),
            result.getRelativeDiff90Percentile(), result.getRelativeDiff90PercentilePercentage());
      }
      logger.println("====================================================================================================================================");
    } else {
      throw new IllegalArgumentException("ConfigType '" + configType + "' is not supported.");
    }
  }

  protected List<PerformanceCalculationResult> calculateDifferenceBetweenCurrentAndPreviousBuilds(Map<String, PerformanceReport> currentPerformanceReportsByFileName, List<AggregatedUriReportCalculationResult> aggregatedUriReportCalculationResults) {
    List<PerformanceCalculationResult> performanceCalculationResults = new ArrayList<PerformanceCalculationResult>();

    PriorityQueue<AggregatedUriReportCalculationResult> resultQueue = new PriorityQueue<AggregatedUriReportCalculationResult>(aggregatedUriReportCalculationResults);

    for (PerformanceReport performanceReport : currentPerformanceReportsByFileName.values()) {
      for (UriReport uriReport : performanceReport.getUriListOrdered()) {
        for (AggregatedUriReportCalculationResult aggregatedUriReportCalculationResult : aggregatedUriReportCalculationResults) {
          if (aggregatedUriReportCalculationResult.getUri().equalsIgnoreCase(uriReport.getStaplerUri())) {
            performanceCalculationResults.add(new PerformanceCalculationResult(uriReport, aggregatedUriReportCalculationResult));
          }
        }
      }
    }

    return performanceCalculationResults;
  }

  protected List<AggregatedUriReportCalculationResult> calculateAggregatedPerformanceResultsForBuilds(List<Map<String, PerformanceReport>> performanceReportsByBuilds) {
    Map<String, List<PerformanceReport>> performanceReportForFile = new HashMap<String, List<PerformanceReport>>();
    List<AggregatedUriReportCalculationResult> aggregatedUriReportCalculationResults = new ArrayList<AggregatedUriReportCalculationResult>();

    for (Map<String, PerformanceReport> performanceReportsByBuild : performanceReportsByBuilds) {
      for (Map.Entry<String, PerformanceReport> performanceReportByFileName : performanceReportsByBuild.entrySet()) {
        List<PerformanceReport> performanceReports = performanceReportForFile.get(performanceReportByFileName.getKey());
        if (performanceReports == null) {
          performanceReports = new ArrayList<PerformanceReport>();
          performanceReportForFile.put(performanceReportByFileName.getKey(), performanceReports);
        }
        performanceReports.add(performanceReportByFileName.getValue());
      }
    }

    for (Map.Entry<String, List<PerformanceReport>> performanceReportEntry : performanceReportForFile.entrySet()) {
      List<PerformanceReport> performanceReports = performanceReportEntry.getValue();

      // aggregate all UriReports by stapler uri for all performance reports for current file
      Map<String, List<UriReport>> uriReportsByStaplerUri = new HashMap<String, List<UriReport>>();
      for (PerformanceReport performanceReport : performanceReports) {
        List<UriReport> uriListOrdered = performanceReport.getUriListOrdered();
        for (UriReport uriReport : uriListOrdered) {
          List<UriReport> uriReports = uriReportsByStaplerUri.get(uriReport.getStaplerUri());
          if (uriReports == null) {
            uriReports = new ArrayList<UriReport>();
            uriReportsByStaplerUri.put(uriReport.getStaplerUri(), uriReports);
          }
          uriReports.add(uriReport);
        }
      }

      // calculate aggregated results for each stapler uri
      for (Map.Entry<String, List<UriReport>> uriReportsForStaplerUri : uriReportsByStaplerUri.entrySet()) {
        List<UriReport> uriReports = uriReportsForStaplerUri.getValue();

        long average = 0;
        long median = 0;
        long percentile = 0;
        for (UriReport uriReport : uriReports) {
          average += uriReport.getAverage();
          median += uriReport.getMedian();
          percentile += uriReport.get90Line();
        }

        long overallAverage = Math.round(average / uriReports.size());
        long overallMedian = Math.round(median / uriReports.size());
        long overall90Percentile = Math.round(percentile / uriReports.size());

        aggregatedUriReportCalculationResults.add(new AggregatedUriReportCalculationResult(uriReportsForStaplerUri.getKey(), overallAverage, overallMedian, overall90Percentile));
      }
    }

    return aggregatedUriReportCalculationResults;
  }

  protected Map<String, PerformanceReport> getPerformanceReportsByFileNameForBuild(BuildListener listener, PrintStream logger, AbstractBuild<?, ?> previousBuild) throws IOException, InterruptedException {
    Map<String, PerformanceReport> previousPerformanceReportsByFileName = new HashMap<String, PerformanceReport>();

    PerformanceBuildAction previousPerformanceBuildAction = new PerformanceBuildAction(previousBuild, logger, parsers);
    previousBuild.addAction(previousPerformanceBuildAction);

    //getting files related to the previous build selected
    logger.println("\n");
    for (PerformanceReportParser parser : parsers) {
      String glob = parser.glob;
      logger.println("Performance: Recording " + parser.getReportName() + " reports '" + glob + "'");

      List<File> localReports = getExistingPerformanceReportsFromBuild(previousBuild, logger, parser.getDescriptor().getDisplayName());
      Collection<PerformanceReport> parsedReports = parser.parse(previousBuild, localReports, listener);

      for (PerformanceReport report : parsedReports) {
        report.setBuildAction(previousPerformanceBuildAction);
        previousPerformanceReportsByFileName.put(report.getReportFileName(), report);
      }
    }
    logger.println("\n");

    return previousPerformanceReportsByFileName;
  }

  protected List<AbstractBuild<?, ?>> getComparisonBuilds(AbstractBuild<?, ?> build, BuildListener listener) throws IOException {
    List<AbstractBuild<?, ?>> previousBuilds = new ArrayList<AbstractBuild<?, ?>>();
    if (compareWithBuildMode.equals(BUILD_MODE_LSB)) {
      previousBuilds.add(build.getPreviousSuccessfulBuild());
    } else if (compareWithBuildMode.equals(BUILD_MODE_LRB)) {
      previousBuilds.add(build.getPreviousCompletedBuild());
    } else if (compareWithBuildMode.equals(BUILD_MODE_LNB)) {
      previousBuilds.addAll(getNthBuilds(build, listener, compareWithBuildMode));
    } else {
      // BUILD_MODE_SB - specific builds
      previousBuilds.addAll(getNthBuilds(build, listener, compareWithBuildMode));
    }
    return previousBuilds;
  }

  /**
   * Decides if the given calculationResult should be discarded because it is below the given minimumThresholdTolerance.
   *
   * @param calculationResult
   * @return true if the result is below the given minimumThresholdTolerance
   */
  protected boolean discardResultBecauseOfMinimumThresholdTolerance(double thresholdTolerance, PerformanceCalculationResult calculationResult) {
    double currentValue = 0.0;
    double previousValue = 0.0;

    if (comparisonType.equalsIgnoreCase(MODE_ART)) {
      currentValue = calculationResult.getCurrentAverage();
      previousValue = calculationResult.getPreviousAverage();
    } else if (comparisonType.equalsIgnoreCase(MODE_MRT)) {
      currentValue = calculationResult.getCurrentMedian();
      previousValue = calculationResult.getPreviousMedian();
    } else if (comparisonType.equalsIgnoreCase(MODE_PRT)) {
      currentValue = calculationResult.getCurrent90Percentile();
      previousValue = calculationResult.getPrevious90Percentile();
    }

    if (currentValue <= thresholdTolerance && previousValue <= thresholdTolerance ) {
      return true;
    }

    return false;
  }

  protected void setBuildOutcomeBasedOnCalculations(AbstractBuild<?, ?> build, List<PerformanceCalculationResult> calculationResults, PrintStream logger) {
    Result result = Result.SUCCESS;

    List<PerformanceCalculationResult> discardedResults = new ArrayList<PerformanceCalculationResult>();
    List<PerformanceCalculationResult> unstableResults = new ArrayList<PerformanceCalculationResult>();
    List<PerformanceCalculationResult> failureResults = new ArrayList<PerformanceCalculationResult>();

    for (PerformanceCalculationResult calculationResult : calculationResults) {
      double relativeDiffPercent = 0.0;

      if (discardResultBecauseOfMinimumThresholdTolerance(minimumThresholdTolerance, calculationResult)) {
        discardedResults.add(calculationResult);
        continue;
      }

      if (comparisonType.equalsIgnoreCase(MODE_ART)) {
        relativeDiffPercent = calculationResult.getRelativeDiffAveragePercentage();
      } else if (comparisonType.equalsIgnoreCase(MODE_MRT)) {
        relativeDiffPercent = calculationResult.getRelativeDiffMedianPercentage();
      } else if (comparisonType.equalsIgnoreCase(MODE_PRT)) {
        relativeDiffPercent = calculationResult.getRelativeDiff90PercentilePercentage();
      }

      if (relativeDiffPercent < 0) {
        if (relativeFailedThresholdNegative >= 0 && Math.abs(relativeDiffPercent) - relativeFailedThresholdNegative > THRESHOLD_TOLERANCE) {
          result = Result.FAILURE;
          failureResults.add(calculationResult);
        } else if (relativeUnstableThresholdNegative >= 0 && Math.abs(relativeDiffPercent) - relativeUnstableThresholdNegative > THRESHOLD_TOLERANCE) {
          result = Result.UNSTABLE;
          unstableResults.add(calculationResult);
        }
      } else if (relativeDiffPercent >= 0) {
        if (relativeFailedThresholdPositive >= 0 && Math.abs(relativeDiffPercent) - relativeFailedThresholdPositive > THRESHOLD_TOLERANCE) {
          result = Result.FAILURE;
          failureResults.add(calculationResult);
        } else if (relativeUnstableThresholdPositive >= 0 && Math.abs(relativeDiffPercent) - relativeUnstableThresholdPositive > THRESHOLD_TOLERANCE) {
          result = Result.UNSTABLE;
          unstableResults.add(calculationResult);
        }
      }

      if (result.isWorseThan(build.getResult())) {
        build.setResult(result);
      }
    }

    if (!unstableResults.isEmpty()) {
      logger.println("\nTests marking build as '" + Result.UNSTABLE + "': ");
      for (PerformanceCalculationResult unstableResult : unstableResults) {
        logger.println(unstableResult.getCurrentStaplerUri() + " -> " + getRelativePercentageBasedOnComparisonType(unstableResult, comparisonType) + "%");
      }
    }
    if (!failureResults.isEmpty()) {
      logger.println("\nTests marking build as '" + Result.FAILURE + "': ");
      for (PerformanceCalculationResult failureResult : failureResults) {
        logger.println(failureResult.getCurrentStaplerUri() + " -> " + getRelativePercentageBasedOnComparisonType(failureResult, comparisonType) + "%");
      }
    }

    if (discardedResults.size() > 0) {
      logger.println("\nDiscarded results (below minimum threshold tolerance of " + minimumThresholdTolerance + "ms):\n");
      StringBuilder sb = new StringBuilder();
      for (PerformanceCalculationResult discardedResult : discardedResults) {
        sb.append(discardedResult.getCurrentStaplerUri() + "\n");
      }
      logger.println(sb.toString());
    }
  }

  protected double getRelativePercentageBasedOnComparisonType(PerformanceCalculationResult result, String comparisonType) {
    if (comparisonType.equalsIgnoreCase(MODE_ART)) {
      return result.getRelativeDiffAveragePercentage();
    } else if (comparisonType.equalsIgnoreCase(MODE_MRT)) {
      return result.getRelativeDiffMedianPercentage();
    } else if (comparisonType.equalsIgnoreCase(MODE_PRT)) {
      return result.getRelativeDiff90PercentilePercentage();
    } else {
      throw new IllegalArgumentException("ComparisonType of " + comparisonType + " is not supported.");
    }
  }

  protected boolean failBuildIfNoFilesArePresent(AbstractBuild<?, ?> build, PrintStream logger, PerformanceReportParser parser, String glob, List<FilePath> files) {
    if (files.isEmpty()) {
      if (build.getResult().isWorseThan(Result.UNSTABLE)) {
        return true;
      }
      build.setResult(Result.FAILURE);
      logger.println("Performance: no " + parser.getReportName() + " files matching '" + glob + "' have been found. " +
          "Has the report generated?. Setting Build to " + build.getResult());
      return true;
    }
    return false;
  }

  protected void logThresholdSettingsForResult(PrintStream logger, Result result, double relativeNegativeThreshold, double relativePositiveThreshold) {
    if (relativeNegativeThreshold <= 100 && relativePositiveThreshold <= 100) {
      logger.println("Performance: Percentage of relative difference outside -"
          + relativeNegativeThreshold + " to +" + relativePositiveThreshold + " % sets the build as "
          + result.toString().toLowerCase());
    } else {
      logger.println("Performance: No threshold configured for making the test "
          + result.toString().toLowerCase());
    }
  }

  protected boolean handleAbsoluteThreshold(AbstractBuild<?, ?> build, BuildListener listener, PrintStream logger, Result result, EnvVars env) {
    String xml = "";

    try {
      List<UriReport> curruriList = null;
      HashMap<String, String> responseTimeThresholdMap = null;

      if (!"".equals(this.errorUnstableResponseTimeThreshold) && this.errorUnstableResponseTimeThreshold != null) {

        responseTimeThresholdMap = new HashMap<String, String>();
        String[] lines = this.errorUnstableResponseTimeThreshold.split("\n");

        for (String line : lines) {
          String[] components = line.split(":");
          if (components.length == 2) {
            logger.println("Setting threshold: " + components[0] + ":" + components[1]);
            responseTimeThresholdMap.put(components[0], components[1]);
          }
        }
      }

      if (errorUnstableThreshold >= 0 && errorUnstableThreshold <= 100) {
        logger.println("Performance: Percentage of errors greater or equal than "
            + errorUnstableThreshold + "% sets the build as "
            + Result.UNSTABLE.toString().toLowerCase());
      } else {
        logger.println("Performance: No threshold configured for making the test "
            + Result.UNSTABLE.toString().toLowerCase());
      }
      if (errorFailedThreshold >= 0 && errorFailedThreshold <= 100) {
        logger.println("Performance: Percentage of errors greater or equal than "
            + errorFailedThreshold + "% sets the build as "
            + Result.FAILURE.toString().toLowerCase());
      } else {
        logger.println("Performance: No threshold configured for making the test "
            + Result.FAILURE.toString().toLowerCase());
      }

      // add the report to the build object.
      PerformanceBuildAction performanceBuildAction = new PerformanceBuildAction(build, logger, parsers);
      build.addAction(performanceBuildAction);
      logger.print("\n\n\n");

      for (PerformanceReportParser parser : parsers) {

        String glob = parser.glob;
        //Replace any runtime environment variables such as ${sample_var}
        glob = env.expand(glob);
        logger.println("Performance: Recording " + parser.getReportName() + " reports '" + glob + "'");

        List<FilePath> files = PerformanceReportUtil.locatePerformanceReports(build.getWorkspace(), glob);

        if (failBuildIfNoFilesArePresent(build, logger, parser, glob, files)) {
          return true;
        }

        List<File> localReports = IoUtil.copyReportsToMaster(build, logger, files, parser.getDescriptor().getDisplayName());
        Collection<PerformanceReport> parsedReports = parser.parse(build, localReports, listener);

        // mark the build as unstable or failure depending on the outcome.
        for (PerformanceReport r : parsedReports) {

          File xmlFile = IoUtil.createXmlFile(build, glob);

          FileWriter fw = new FileWriter(xmlFile.getAbsoluteFile());
          BufferedWriter bw = new BufferedWriter(fw);

          xml = "<?xml version=\"1.0\"?>\n";
          xml += "<results>\n";
          xml += "<absoluteDefinition>\n";

          String unstable = "\t<unstable>";
          String failed = "\t<failed>";
          String calc = "\t<calculated>";

          unstable += errorUnstableThreshold;
          failed += errorFailedThreshold;

          String avg = "", med = "", perct = "";

          avg += "<average>\n";
          med += "<median>\n";
          perct += "<percentile>\n";

          r.setBuildAction(performanceBuildAction);
          double errorPercent = r.errorPercent();
          calc += errorPercent;

          curruriList = r.getUriListOrdered();

          if (errorFailedThreshold >= 0 && errorPercent - errorFailedThreshold > THRESHOLD_TOLERANCE) {
            result = Result.FAILURE;
            build.setResult(Result.FAILURE);
          } else if (errorUnstableThreshold >= 0 && errorPercent - errorUnstableThreshold > THRESHOLD_TOLERANCE) {
            result = Result.UNSTABLE;
          }

          long average = r.getAverage();
          logger.println(r.getReportFileName() + " has an average of: " + Long.toString(average));

          try {
            if (responseTimeThresholdMap != null && responseTimeThresholdMap.get(r.getReportFileName()) != null) {
              if (Long.parseLong(responseTimeThresholdMap.get(r.getReportFileName())) <= average) {
                logger.println("UNSTABLE: " + r.getReportFileName() + " has exceeded the threshold of [" + Long.parseLong(responseTimeThresholdMap.get(r.getReportFileName())) + "] with the time of [" + Long.toString(average) + "]");
                result = Result.UNSTABLE;
              }
            }
          } catch (NumberFormatException nfe) {
            logger.println("ERROR: Threshold set to performanceBuildAction non-number [" + responseTimeThresholdMap.get(r.getReportFileName()) + "]");
            result = Result.FAILURE;
            build.setResult(Result.FAILURE);

          }
          if (result.isWorseThan(build.getResult())) {
            build.setResult(result);
          }
          logger.println("Performance: File " + r.getReportFileName()
              + " reported " + errorPercent
              + "% of errors [" + result + "]. Build status is: "
              + build.getResult());

          for (int i = 0; i < curruriList.size(); i++) {
            avg += "\t<" + curruriList.get(i).getStaplerUri() + ">\n";
            avg += "\t\t<currentBuildAvg>" + curruriList.get(i).getAverage() + "</currentBuildAvg>\n";
            avg += "\t</" + curruriList.get(i).getStaplerUri() + ">\n";


            med += "\t<" + curruriList.get(i).getStaplerUri() + ">\n";
            med += "\t\t<currentBuildMed>" + curruriList.get(i).getMedian() + "</currentBuildMed>\n";
            med += "\t</" + curruriList.get(i).getStaplerUri() + ">\n";


            perct += "\t<" + curruriList.get(i).getStaplerUri() + ">\n";
            perct += "\t\t<currentBuild90Line>" + curruriList.get(i).get90Line() + "</currentBuild90Line>\n";
            perct += "\t</" + curruriList.get(i).getStaplerUri() + ">\n";

          }
          unstable += "</unstable>";
          failed += "</failed>";
          calc += "</calculated>";

          avg += "</average>\n";
          med += "</median>\n";
          perct += "</percentile>\n";

          xml += unstable + "\n";
          xml += failed + "\n";
          xml += calc + "\n";
          xml += "</absoluteDefinition>\n";

          xml += avg;
          xml += med;
          xml += perct;
          xml += "</results>";

          bw.write(xml);
          bw.close();
          fw.close();

          logger.print("\n\n\n");
        }
      }
    } catch (Exception e) {
    }
    return false;
  }

  public Object readResolve() {
    // data format migration
    if (parsers == null) {
      parsers = new ArrayList<PerformanceReportParser>();
    }
    if (filename != null) {
      parsers.add(new JMeterParser(filename));
      filename = null;
    }
    return this;
  }

  public int getErrorFailedThreshold() {
    return errorFailedThreshold;
  }

  public void setErrorFailedThreshold(int errorFailedThreshold) {
    this.errorFailedThreshold = Math.max(0, Math.min(errorFailedThreshold, 100));
  }

  public int getErrorUnstableThreshold() {
    return errorUnstableThreshold;
  }

  public void setErrorUnstableThreshold(int errorUnstableThreshold) {
    this.errorUnstableThreshold = Math.max(0, Math.min(errorUnstableThreshold,
        100));
  }

  public String getErrorUnstableResponseTimeThreshold() {
    return this.errorUnstableResponseTimeThreshold;
  }

  public void setErrorUnstableResponseTimeThreshold(String errorUnstableResponseTimeThreshold) {
    this.errorUnstableResponseTimeThreshold = errorUnstableResponseTimeThreshold;
  }

  public boolean isModePerformancePerTestCase() {
    return modePerformancePerTestCase;
  }

  public void setModePerformancePerTestCase(boolean modePerformancePerTestCase) {
    this.modePerformancePerTestCase = modePerformancePerTestCase;
  }

  public boolean getModePerformancePerTestCase() {
    return modePerformancePerTestCase;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }


  public boolean isART() {
    return comparisonType.compareToIgnoreCase(PerformancePublisher.MODE_ART) == 0;
  }

  public boolean isMRT() {
    return comparisonType.compareToIgnoreCase(PerformancePublisher.MODE_MRT) == 0;
  }

  public boolean isPRT() {
    return comparisonType.compareToIgnoreCase(PerformancePublisher.MODE_PRT) == 0;
  }

  protected int[] parseBuildNumbers() {
    AssertHelper.checkNotNull("Compare build numbers are not set.", compareWithBuildNumbers);
    String[] buildNumbersAsString = compareWithBuildNumbers.split("[\\s,]");
    int[] buildNumbers = new int[buildNumbersAsString.length];
    for (int i = 0; i < buildNumbersAsString.length; i++) {
      buildNumbers[i] = Integer.parseInt(buildNumbersAsString[i]);
    }

    return buildNumbers;
  }

  public List<AbstractBuild<?, ?>> getNthBuilds(AbstractBuild<?, ?> build, BuildListener listener, String buildMode) throws IOException {
    List<AbstractBuild<?, ?>> previousBuilds = new ArrayList<AbstractBuild<?, ?>>();

    int[] buildNumbers = parseBuildNumbers();
    if (buildMode.equals(BUILD_MODE_LNB)) {
      if (buildNumbers.length == 0 || buildNumbers.length > 1) {
        throw new IllegalArgumentException("Unable to determine correct build number.");
      }

      AbstractBuild<?, ?> previousBuild = build;
      for (int i = 0; i < buildNumbers[0]; i++) {
        AbstractBuild<?, ?> tempBuild = previousBuild.getPreviousBuild();
        previousBuilds.add(tempBuild);
        previousBuild = tempBuild;
      }
    } else if (buildMode.equals(BUILD_MODE_SB)) {
      for (int buildNumber : buildNumbers) {
        AbstractBuild<?, ?> previousBuild = build.getProject().getBuildByNumber(buildNumber);
        previousBuilds.add(previousBuild);
      }
    } else {
      throw new IllegalArgumentException("Build mode: '" + buildMode + "' is not supported.");
    }

    return previousBuilds;
  }

  protected List<File> getExistingPerformanceReportsFromBuild(AbstractBuild<?, ?> build, PrintStream logger, String parserDisplayName)
      throws IOException, InterruptedException {
    List<File> localReports = new ArrayList<File>();
    final File localReport[] = PerformanceReportUtil.getPerformanceReportsFromBuildDirectory(build, parserDisplayName);

    for (int i = 0; i < localReport.length; i++) {
      String name = localReport[i].getName();
      String[] arr = name.split("\\.");

      // skip the serialized report files
      if (arr[arr.length - 1].equalsIgnoreCase("serialized")) {
        continue;
      }

      localReports.add(localReport[i]);
    }

    if (localReports.isEmpty()) {
      logger.println("Build " + build.getDisplayName() + " contains no performance reports for parser '" + parserDisplayName + "'");
    }

    return localReports;
  }

  public double getRelativeFailedThresholdPositive() {
    return relativeFailedThresholdPositive;
  }

  public double getRelativeFailedThresholdNegative() {
    return relativeFailedThresholdNegative;
  }

  public void setRelativeFailedThresholdPositive(double relativeFailedThresholdPositive) {
    this.relativeFailedThresholdPositive = Math.max(0, Math.min(relativeFailedThresholdPositive, 100));
  }

  public void setRelativeFailedThresholdNegative(double relativeFailedThresholdNegative) {
    this.relativeFailedThresholdNegative = Math.max(0, Math.min(relativeFailedThresholdNegative, 100));
  }

  public double getRelativeUnstableThresholdPositive() {
    return relativeUnstableThresholdPositive;
  }

  public double getRelativeUnstableThresholdNegative() {
    return relativeUnstableThresholdNegative;
  }

  public void setRelativeUnstableThresholdPositive(double relativeUnstableThresholdPositive) {
    this.relativeUnstableThresholdPositive = Math.max(0, Math.min(relativeUnstableThresholdPositive,
        100));
  }

  public void setRelativeUnstableThresholdNegative(double relativeUnstableThresholdNegative) {
    this.relativeUnstableThresholdNegative = Math.max(0, Math.min(relativeUnstableThresholdNegative,
        100));
  }

  public String getComparisonType() {
    return comparisonType;
  }

  public void setComparisonType(String comparisonType) {
    this.comparisonType = comparisonType;
  }

  public boolean getThresholdMode() {
    return thresholdMode;
  }

  public void setThresholdMode(boolean thresholdMode) {
    this.thresholdMode = thresholdMode;
  }

  public boolean isModeThroughput() {
    return modeThroughput;
  }

  public void setModeThroughput(boolean modeThroughput) {
    this.modeThroughput = modeThroughput;
  }

  public String getCompareWithBuildMode() {
    return compareWithBuildMode;
  }

  public void setCompareWithBuildMode(String compareWithBuildMode) {
    this.compareWithBuildMode = compareWithBuildMode;
  }

  public String getCompareWithBuildNumbers() {
    return compareWithBuildNumbers;
  }

  public void setCompareWithBuildNumbers(String compareWithBuildNumbers) {
    this.compareWithBuildNumbers = compareWithBuildNumbers;
  }

  public int getMinimumThresholdTolerance() {
    return minimumThresholdTolerance;
  }

  public void setMinimumThresholdTolerance(int minimumThresholdTolerance) {
    this.minimumThresholdTolerance = minimumThresholdTolerance;
  }
}


