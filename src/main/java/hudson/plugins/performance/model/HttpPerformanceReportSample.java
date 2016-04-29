package hudson.plugins.performance.model;

import hudson.plugins.performance.report.UriReport;

/**
 * Information about a particular HTTP request and how that went.
 * 
 * This object belongs under {@link UriReport}.
 */
public class HttpPerformanceReportSample extends PerformanceReportSample<HttpPerformanceReportSample> {

  private String httpCode = "";
	private double sizeInKb;

	// Summarizer fields
	private long summarizerMin;
	private long summarizerMax;
	private float summarizerErrors;
	private long summarizerSamples;

	public String getHttpCode() {
		return httpCode;
	}

	public long getSummarizerSamples() {
		return summarizerSamples;
	}

	public long getSummarizerMin() {
		return summarizerMin;
	}

	public long getSummarizerMax() {
		return summarizerMax;
	}

	public float getSummarizerErrors() {
		return summarizerErrors;
	}

	public void setHttpCode(String httpCode) {
		this.httpCode = httpCode;
	}

	public void setSummarizerSamples(long summarizerSamples) {
		this.summarizerSamples = summarizerSamples;
	}

	public void setSummarizerMin(long summarizerMin) {
		this.summarizerMin = summarizerMin;
	}

	public void setSummarizerMax(long summarizerMax) {
		this.summarizerMax = summarizerMax;
	}

	public void setSummarizerErrors(float summarizerErrors) {
		this.summarizerErrors = summarizerErrors;
	}

	public double getSizeInKb() {
		return sizeInKb;
	}

	public void setSizeInKb(double d) {
		this.sizeInKb = d;
	}

  @Override
  public int compareTo(HttpPerformanceReportSample o) {
    return (int) (getDuration() - o.getDuration());
  }

}
