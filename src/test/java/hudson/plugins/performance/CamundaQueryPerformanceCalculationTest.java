package hudson.plugins.performance;

import hudson.plugins.performance.parser.CamundaQueryPerformanceParser;
import hudson.plugins.performance.report.PerformanceReport;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class CamundaQueryPerformanceCalculationTest {

  @Test
  public void calculateAggregationPerformanceResultForOnePerformanceReport() throws Exception {
    PerformancePublisher performancePublisher = getPerformancePublisherFixture(PerformancePublisher.MODE_ART);

    Map<String, PerformanceReport> fixtures = singlePerformanceReportFixture();

    List<AggregatedUriReportCalculationResult> aggregatedUriReportCalculationResults = performancePublisher.calculateAggregatedPerformanceResultsForBuilds(Arrays.asList(fixtures));
    assertEquals(1, aggregatedUriReportCalculationResults.size());
    assertEquals(395, aggregatedUriReportCalculationResults.get(0).getAverage());
    assertEquals(395, aggregatedUriReportCalculationResults.get(0).getMedian());
    assertEquals(395, aggregatedUriReportCalculationResults.get(0).getPercentile());
  }

  @Test
  public void calculateAggregationPerformanceResultsForMultiplePerformanceReports() throws Exception {
    PerformancePublisher performancePublisher = getPerformancePublisherFixture(PerformancePublisher.MODE_ART);

    Map<String, PerformanceReport> fixtures = multiplePerformanceReportFixtures_Slow();

    List<AggregatedUriReportCalculationResult> aggregatedUriReportCalculationResults = performancePublisher.calculateAggregatedPerformanceResultsForBuilds(Arrays.asList(fixtures));
    assertEquals(4, aggregatedUriReportCalculationResults.size());
    assertEquals(3405, aggregatedUriReportCalculationResults.get(0).getAverage());
    assertEquals(3405, aggregatedUriReportCalculationResults.get(0).getMedian());
    assertEquals(3405, aggregatedUriReportCalculationResults.get(0).getPercentile());
  }

  @Test
  public void calculateDifferenceAggregationPerformanceResultsForMultiplePerformanceReports() throws Exception {
    PerformancePublisher performancePublisher = getPerformancePublisherFixture(PerformancePublisher.MODE_ART);

    Map<String, PerformanceReport> previousBuildPerformanceReports = multiplePerformanceReportFixtures_Slow();
    Map<String, PerformanceReport> currentBuildPerformanceReports = multiplePerformanceReportFixtures_Fast();

    assertEquals(4, previousBuildPerformanceReports.size());
    assertEquals(4, currentBuildPerformanceReports.size());

    List<AggregatedUriReportCalculationResult> aggregatedPreviousResults = performancePublisher.calculateAggregatedPerformanceResultsForBuilds(Arrays.asList(previousBuildPerformanceReports));
    assertEquals(4, aggregatedPreviousResults.size());

    List<PerformanceCalculationResult> performanceDifference = performancePublisher.calculateDifferenceBetweenCurrentAndPreviousBuilds(currentBuildPerformanceReports, aggregatedPreviousResults);
    for (PerformanceCalculationResult performanceCalculationResult : performanceDifference) {

      assertEquals(performanceCalculationResult.getCurrentStaplerUri(), performanceCalculationResult.getPreviousStaplerUri());

      PerformanceReport performanceReportCurrentBuild = currentBuildPerformanceReports.get(performanceCalculationResult.getCurrentStaplerUri() + ".json");
      assertEquals(performanceReportCurrentBuild.getAverage(), performanceCalculationResult.getCurrentAverage());
      assertEquals(performanceReportCurrentBuild.getMedian(), performanceCalculationResult.getCurrentMedian());
      assertEquals(performanceReportCurrentBuild.get90Line(), performanceCalculationResult.getCurrent90Percentile());

      PerformanceReport performanceReportPreviousBuild = previousBuildPerformanceReports.get(performanceCalculationResult.getPreviousStaplerUri() + ".json");
      assertEquals(performanceReportPreviousBuild.getAverage(), performanceCalculationResult.getPreviousAverage());
      assertEquals(performanceReportPreviousBuild.getMedian(), performanceCalculationResult.getPreviousMedian());
      assertEquals(performanceReportPreviousBuild.get90Line(), performanceCalculationResult.getPrevious90Percentile());
    }
  }

  protected Map<String, PerformanceReport> singlePerformanceReportFixture() throws Exception {
    File[] files = new File[] { new File("src/test/resources/camundaqueryperformance/HistoryPerformanceTest.testQuery.json") };
    return createPerformanceReportFixturesForCamundaPerformanceQuery(files);
  }

  protected Map<String, PerformanceReport> multiplePerformanceReportFixtures_Slow() throws Exception {
    File[] reportFiles = new File("src/test/resources/camundaqueryperformance/build_1_slow/").listFiles();
    return createPerformanceReportFixturesForCamundaPerformanceQuery(reportFiles);
  }

  protected Map<String, PerformanceReport> multiplePerformanceReportFixtures_Fast() throws Exception {
    File[] reportFiles = new File("src/test/resources/camundaqueryperformance/build_2_fast/").listFiles();
    return createPerformanceReportFixturesForCamundaPerformanceQuery(reportFiles);
  }

  protected List<Map<String, PerformanceReport>> multipleBuildPerformanceReportFixtures_Fast() throws Exception {
    File[] reportFilesBuild2 = new File("src/test/resources/camundaqueryperformance/build_2_fast/").listFiles();
    File[] reportFilesBuild3 = new File("src/test/resources/camundaqueryperformance/build_3_fast/").listFiles();

    Map<String, PerformanceReport> fixturesReportFiles = createPerformanceReportFixturesForCamundaPerformanceQuery(reportFilesBuild2);
    Map<String, PerformanceReport> fixturesReportFilesBuild3 = createPerformanceReportFixturesForCamundaPerformanceQuery(reportFilesBuild3);

    return Arrays.asList(fixturesReportFiles, fixturesReportFilesBuild3);

  }

  protected List<Map<String, PerformanceReport>> multipleBuildPerformanceReportFixtures_Mixed() throws Exception {
    File[] reportFilesSlow = new File("src/test/resources/camundaqueryperformance/build_1_slow/").listFiles();
    File[] reportFilesFast = new File("src/test/resources/camundaqueryperformance/build_2_fast/").listFiles();

    Map<String, PerformanceReport> fixturesReportFiles = createPerformanceReportFixturesForCamundaPerformanceQuery(reportFilesSlow);
    Map<String, PerformanceReport> fixturesReportFilesFast = createPerformanceReportFixturesForCamundaPerformanceQuery(reportFilesFast);

    return Arrays.asList(fixturesReportFiles, fixturesReportFilesFast);
  }

  protected Map<String, PerformanceReport> createPerformanceReportFixturesForCamundaPerformanceQuery(File[] reportFiles) throws Exception {
    CamundaQueryPerformanceParser parser = new CamundaQueryPerformanceParser(CamundaQueryPerformanceParser.DEFAULT_GLOB_PATTERN);

    Map<String, PerformanceReport> performanceReportsByFileName = new HashMap<String, PerformanceReport>();

    for (File reportFile : reportFiles) {
      PerformanceReport performanceReport = parser.parse(reportFile);
      performanceReportsByFileName.put(performanceReport.getReportFileName(), performanceReport);
    }

    return performanceReportsByFileName;
  }

  protected PerformancePublisher getPerformancePublisherFixture(String modeArt) {
    return new PerformancePublisher(
        0, 0, "test.jtl:5000",
        0, 0, 0, 0,
        PerformancePublisher.BUILD_MODE_LSB, null, 0,
        false, modeArt, false,
        asList(new CamundaQueryPerformanceParser(CamundaQueryPerformanceParser.DEFAULT_GLOB_PATTERN)),
        false);
  }

  protected static <T> T[] concatAll(T[] first, T[]... rest) {
    int totalLength = first.length;
    for (T[] array : rest) {
      totalLength += array.length;
    }
    T[] result = Arrays.copyOf(first, totalLength);
    int offset = first.length;
    for (T[] array : rest) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

}
