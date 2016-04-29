package hudson.plugins.performance.util;

import hudson.model.AbstractBuild;
import hudson.plugins.performance.PerformanceCalculationResult;
import hudson.plugins.performance.PerformancePublisher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DashboardFile {

  public static String createRelativeDiff90PercentileXmlElement(String currentStaplerUri, long current90Percentile, long previous90Percentile, double relativeDiff90Percentile, double relativeDiffPercent90Percentile) {
    String element = "\t<" + currentStaplerUri + ">\n";
    element += "\t\t<previousBuild90Line>" + previous90Percentile + "</previousBuild90Line>\n";
    element += "\t\t<currentBuild90Line>" + current90Percentile + "</currentBuild90Line>\n";
    element += "\t\t<relativeDiff>" + relativeDiff90Percentile + "</relativeDiff>\n";
    element += "\t\t<relativeDiffPercent>" + relativeDiffPercent90Percentile + "</relativeDiffPercent>\n";
    element += "\t</" + currentStaplerUri + ">\n";
    return element;
  }

  public static String createRelativeDiffMedianXmlElement(String currentStaplerUri, long currentMedian, long previousMedian, double relativeDiffMedian, double relativeDiffPercentMedian) {
    String element = "\t<" + currentStaplerUri + ">\n";
    element += "\t\t<previousBuildMed>" + previousMedian + "</previousBuildMed>\n";
    element += "\t\t<currentBuildMed>" + currentMedian + "</currentBuildMed>\n";
    element += "\t\t<relativeDiff>" + relativeDiffMedian + "</relativeDiff>\n";
    element += "\t\t<relativeDiffPercent>" + relativeDiffPercentMedian + "</relativeDiffPercent>\n";
    element += "\t</" + currentStaplerUri + ">\n";
    return element;
  }

  public static String createRelativeDiffAverageXmlElement(String currentStaplerUri, long currentAverage, long previousAverage, double relativeDiffAverage, double relativeDiffPercentAverage) {
    String element = "\t<" + currentStaplerUri + ">\n";
    element += "\t\t<previousBuildAvg>" + previousAverage + "</previousBuildAvg>\n";
    element += "\t\t<currentBuildAvg>" + currentAverage + "</currentBuildAvg>\n";
    element += "\t\t<relativeDiff>" + relativeDiffAverage + "</relativeDiff>\n";
    element += "\t\t<relativeDiffPercent>" + relativeDiffPercentAverage + "</relativeDiffPercent>\n";
    element += "\t</" + currentStaplerUri + ">\n";
    return element;
  }

  public static String createXmlElementFromPerformanceCalculationResult(String calculationType, PerformanceCalculationResult result) {
    AssertHelper.checkNotNull(calculationType);
    if (calculationType.equalsIgnoreCase(PerformancePublisher.MODE_ART)) {
      return createRelativeDiffAverageXmlElement(result.getCurrentStaplerUri(), result.getCurrentAverage(), result.getPreviousAverage(), result.getRelativeDiffAverage(), result.getRelativeDiffAveragePercentage());
    } else if (calculationType.equalsIgnoreCase(PerformancePublisher.MODE_MRT)) {
      return createRelativeDiffMedianXmlElement(result.getCurrentStaplerUri(), result.getCurrentMedian(), result.getPreviousMedian(), result.getRelativeDiffMedian(), result.getRelativeDiffMedianPercentage());
    } else if (calculationType.equalsIgnoreCase(PerformancePublisher.MODE_PRT)) {
      return createRelativeDiff90PercentileXmlElement(result.getCurrentStaplerUri(), result.getCurrent90Percentile(), result.getPrevious90Percentile(), result.getRelativeDiff90Percentile(), result.getRelativeDiff90PercentilePercentage());
    } else {
      throw new IllegalArgumentException("Calculation of type: '" + calculationType + "' is not supported!");
    }
  }

  public static void writeAsXmlFile(double relativeFailedThresholdNegative, double relativeFailedThresholdPositive, double relativeUnstableThresholdNegative, double relativeUnstableThresholdPositive, AbstractBuild<?, ?> build, String fileName, List<PerformanceCalculationResult> performanceCalculationResults) {
    String buildNumber = "\t<buildNum>" + build.getNumber() + "</buildNum>\n";
    String unstableThresholds = "\t<unstableThresholds>\n\t\t<negative>" + relativeUnstableThresholdNegative + "</negative>\n\t\t<positive>" + relativeUnstableThresholdPositive + "</positive>\n\t</unstableThresholds>\n";
    String failedThresholds = "\t<failedThresholds>\n\t\t<negative>" + relativeFailedThresholdNegative + "</negative>\n\t\t<positive>" + relativeFailedThresholdPositive + "</positive>\n\t</failedThresholds>\n";
    String relativeDefinition = "<relativeDefinition>\n" + buildNumber + unstableThresholds + failedThresholds + "</relativeDefinition>\n";

    StringBuilder average = new StringBuilder().append("<average>\n");
    StringBuilder median = new StringBuilder().append("<median>\n");
    StringBuilder percentile = new StringBuilder().append("<percentile>\n");

    // iterate over performanceCalculationResults to create average, median and percentile elements
    if (performanceCalculationResults != null) {
      for (PerformanceCalculationResult performanceCalculationResult : performanceCalculationResults) {
        average.append(createXmlElementFromPerformanceCalculationResult(PerformancePublisher.MODE_ART, performanceCalculationResult));
        median.append(createXmlElementFromPerformanceCalculationResult(PerformancePublisher.MODE_MRT, performanceCalculationResult));
        percentile.append(createXmlElementFromPerformanceCalculationResult(PerformancePublisher.MODE_PRT, performanceCalculationResult));
      }
    }

    average.append("</average>\n");
    median.append("</median>\n");
    percentile.append("</percentile>");

    FileWriter fw = null;
    BufferedWriter bw = null;
    try {
      File xmlFile = IoUtil.createXmlFile(build, fileName);
      fw = new FileWriter(xmlFile.getAbsoluteFile());
      bw = new BufferedWriter(fw);

      bw.write("<?xml version=\"1.0\"?>\n");
      bw.write("<results>\n");
      bw.write(relativeDefinition);
      bw.write(average.toString());
      bw.write(median.toString());
      bw.write(percentile.toString() + "\n");
      bw.write("</results>");
    } catch (IOException e) {
      throw new RuntimeException("Unable to write dashboard xml file.", e);
    } finally {
      IoUtil.closeSilently(bw);
      IoUtil.closeSilently(fw);
    }
  }
}
