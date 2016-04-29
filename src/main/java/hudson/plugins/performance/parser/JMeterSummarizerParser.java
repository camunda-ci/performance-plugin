package hudson.plugins.performance.parser;

import hudson.Extension;
import hudson.plugins.performance.model.HttpPerformanceReportSample;
import hudson.plugins.performance.report.PerformanceReport;
import hudson.plugins.performance.util.IoUtil;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Parses JMeter Summarized results
 *
 * @author Agoley
 */
public class JMeterSummarizerParser extends AbstractParser {

  public final String logDateFormat;

  @Extension
  public static class DescriptorImpl extends PerformanceReportParserDescriptor {
    @Override
    public String getDisplayName() {
      return "JMeterSummarizer";
    }
  }

  @DataBoundConstructor
  public JMeterSummarizerParser(String glob, String logDateFormat) {
    super(glob);

    if (logDateFormat == null || logDateFormat.length() == 0) {
      this.logDateFormat = getDefaultDatePattern();
    } else {
      this.logDateFormat = logDateFormat;
    }
  }

  @Override
  public String getDefaultGlobPattern() {
    return "**/*.log";
  }

  public String getDefaultDatePattern() {
    return "yyyy/mm/dd HH:mm:ss";
  }

  @Override
  public PerformanceReport parse(File reportFile) throws Exception {
    final PerformanceReport report = new PerformanceReport();
    report.setReportFileName(reportFile.getName());
    report.setReportFileName(reportFile.getName());
    
    Scanner s = null;
    try {
      s = new Scanner(reportFile);
      String key;
      String line;
      SimpleDateFormat dateFormat = new SimpleDateFormat(logDateFormat);

      while (s.hasNextLine()) {
        line = s.nextLine().replaceAll("=", " ");
        if (line.contains("+") && line.contains("jmeter.reporters.Summariser:")) {
          Scanner scanner = null;
          try {
            scanner = new Scanner(line);
            final Pattern delimiter = scanner.delimiter();
            scanner.useDelimiter("INFO"); // as jmeter logs INFO mode
            final HttpPerformanceReportSample sample = new HttpPerformanceReportSample();
            final String dateString = scanner.next();
            sample.setDate(dateFormat.parse(dateString));
            scanner.findInLine("jmeter.reporters.Summariser:");
            scanner.useDelimiter("\\+");
            key = scanner.next().trim();
            scanner.useDelimiter(delimiter);
            scanner.next();
            sample.setSummarizerSamples(scanner.nextLong()); // set SamplesCount
            scanner.findInLine("Avg:"); // set response time
            sample.setDuration(scanner.nextLong());
            sample.setSuccessful(true);
            scanner.findInLine("Min:"); // set MIN
            sample.setSummarizerMin(scanner.nextLong());
            scanner.findInLine("Max:"); // set MAX
            sample.setSummarizerMax(scanner.nextLong());
            scanner.findInLine("Err:"); // set errors count
            sample.setSummarizerErrors(scanner.nextInt());
            // sample.setSummarizerErrors(
            // Float.valueOf(scanner.next().replaceAll("[()%]","")));
            sample.setUri(key);
            report.addSample(sample);
          } finally {
            IoUtil.closeSilently(scanner);
          }
        }
      }
      
      return report;
    } finally {
      IoUtil.closeSilently(s);
    }
  }
}