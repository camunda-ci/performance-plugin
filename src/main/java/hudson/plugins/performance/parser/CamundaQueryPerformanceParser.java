package hudson.plugins.performance.parser;

import com.google.gson.Gson;
import hudson.Extension;
import hudson.plugins.performance.model.HttpPerformanceReportSample;
import hudson.plugins.performance.report.PerformanceReport;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CamundaQueryPerformanceParser extends AbstractParser {

  public static final String DEFAULT_GLOB_PATTERN = "**/target/**/*.json";

  @Extension
  public static class DescriptorImpl extends PerformanceReportParserDescriptor {

    @Override
    public String getDisplayName() {
      return "CamundaQueryPerformance";
    }

  }

  @DataBoundConstructor
  public CamundaQueryPerformanceParser(String glob) {
    super(glob);
  }

  @Override
  public String getDefaultGlobPattern() {
    return DEFAULT_GLOB_PATTERN;
  }

  public PerformanceReport parse(File reportFile) throws Exception {
    return parseCamundaQueryPerformanceJson(reportFile);
  }

  protected PerformanceReport parseCamundaQueryPerformanceJson(File reportFile) throws FileNotFoundException, SAXException {
    final PerformanceReport report = new PerformanceReport();
    report.setReportFileName(reportFile.getName());

    BufferedReader bufferedReader = new BufferedReader(new FileReader(reportFile));
    Map<String,Object> result = new Gson().fromJson(bufferedReader, Map.class);

    HttpPerformanceReportSample sample = new HttpPerformanceReportSample();
    sample.setUri((String) result.get("testName"));
    sample.setDate(new Date(0));
    List<Map<String,Object>> passResults = (List<Map<String, Object>>) result.get("passResults");
    Map<String,Object> stepResult = passResults.get(0);
    sample.setDuration(((Double) stepResult.get("duration")).longValue());
    sample.setSuccessful(true);
    sample.setErrorObtained(false);

    report.addSample(sample);

    return report;
  }

}
