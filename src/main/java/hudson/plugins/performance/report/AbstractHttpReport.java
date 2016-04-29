package hudson.plugins.performance.report;

public abstract class AbstractHttpReport extends AbstractReport {

  abstract public String getHttpCode();

  abstract public String getLastBuildHttpCodeIfChanged();
}
