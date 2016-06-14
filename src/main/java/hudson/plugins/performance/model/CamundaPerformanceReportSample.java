package hudson.plugins.performance.model;

/**
 * @author Christian Lipphardt
 */
public class CamundaPerformanceReportSample extends PerformanceReportSample<CamundaPerformanceReportSample>{

  @Override
  public int compareTo(CamundaPerformanceReportSample o) {
    return (int) (getDuration() - o.getDuration());
  }

}
