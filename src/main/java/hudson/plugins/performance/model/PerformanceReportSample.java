package hudson.plugins.performance.model;

import java.io.Serializable;
import java.util.Date;

public abstract class PerformanceReportSample<T> implements Serializable, Comparable<T> {

  private static final long serialVersionUID = 1L;

  protected Date date;
  protected long duration;
  protected boolean errorObtained;
  protected boolean successful;
  protected String uri;

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public boolean isErrorObtained() {
    return errorObtained;
  }

  public void setErrorObtained(boolean errorObtained) {
    this.errorObtained = errorObtained;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public void setSuccessful(boolean successful) {
    this.successful = successful;
  }

  public boolean isFailed() {
    return !isSuccessful();
  }

  public boolean hasError() {
    return isErrorObtained();
  }

}
