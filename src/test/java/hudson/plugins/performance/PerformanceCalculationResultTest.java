package hudson.plugins.performance;

import hudson.plugins.performance.model.HttpPerformanceReportSample;
import hudson.plugins.performance.report.UriReport;
import hudson.plugins.performance.util.URIHelper;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class PerformanceCalculationResultTest {

  @Test
  public void testPerformanceCalculationResultCalculation() {
    String uri = "MyTestClass.myTestMethod";
    int currentDuration = 2000;
    int previousDuration = 3000;

    long difference = currentDuration - previousDuration;
    double differenceInPercent= Math.round((((2000d-3000d) * 100) / 3000d) * 100) / 100;

    UriReport currentUriReport = constructUriReportFixture(uri, currentDuration);
    UriReport previousUriReport = constructUriReportFixture(uri, previousDuration);

    PerformanceCalculationResult performanceCalculationResult = new PerformanceCalculationResult(currentUriReport, previousUriReport);

    assertEquals(uri, performanceCalculationResult.getCurrentStaplerUri());
    assertEquals(uri, performanceCalculationResult.getPreviousStaplerUri());

    assertEquals(currentDuration, performanceCalculationResult.getCurrentAverage());
    assertEquals(previousDuration, performanceCalculationResult.getPreviousAverage());
    assertEquals(difference, performanceCalculationResult.getRelativeDiffAverage(), Double.MIN_VALUE);
    assertEquals(differenceInPercent, performanceCalculationResult.getRelativeDiffAveragePercentage(), Double.MIN_VALUE);

    assertEquals(currentDuration, performanceCalculationResult.getCurrentMedian());
    assertEquals(previousDuration, performanceCalculationResult.getPreviousMedian());
    assertEquals(difference, performanceCalculationResult.getRelativeDiffMedian(), Double.MIN_VALUE);
    assertEquals(differenceInPercent, performanceCalculationResult.getRelativeDiffMedianPercentage(), Double.MIN_VALUE);

    assertEquals(currentDuration, performanceCalculationResult.getCurrent90Percentile());
    assertEquals(previousDuration, performanceCalculationResult.getPrevious90Percentile());
    assertEquals(difference, performanceCalculationResult.getRelativeDiff90Percentile(), Double.MIN_VALUE);
    assertEquals(differenceInPercent, performanceCalculationResult.getRelativeDiff90PercentilePercentage(), Double.MIN_VALUE);
  }

  protected UriReport constructUriReportFixture(String uri, int duration) {
    HttpPerformanceReportSample sample = new HttpPerformanceReportSample();
    sample.setUri(uri);
    sample.setSuccessful(true);
    sample.setDuration(duration);
    sample.setDate(new Date(0));
    sample.setErrorObtained(false);

    UriReport uriReport = new UriReport(URIHelper.asStaplerURI(uri), uri);
    uriReport.addHttpSample(sample);
    return uriReport;
  }

}
