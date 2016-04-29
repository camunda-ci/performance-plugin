package hudson.plugins.performance.util;

/**
 * Represents a range of builds
 */
public class BuildRange {

  public int first;

  public int last;

  public int step;

  public BuildRange(int first, int last) {
    this.first = first;
    this.last = last;
    this.step = 1;
  }

  public BuildRange(int first, int last, int step) {
    this(first, last);
    this.step = step;
  }

  public boolean in(int nbBuildsToAnalyze) {
    return nbBuildsToAnalyze <= last && first <= nbBuildsToAnalyze;
  }

  public boolean includedByStep(int buildNumber) {
    return buildNumber % step == 0;
  }

}
