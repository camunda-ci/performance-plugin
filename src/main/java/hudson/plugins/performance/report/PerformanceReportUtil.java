package hudson.plugins.performance.report;

import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerformanceReportUtil {

  public static File getPerformanceReport(AbstractBuild<?, ?> build, String parserDisplayName, String performanceReportName) {
    return new File(build.getRootDir(), PerformanceReportMap.getPerformanceReportFileRelativePath(parserDisplayName, getPerformanceReportBuildFileName(performanceReportName)));
  }

  /**
   * <p>
   * Delete the date suffix appended to the Performance result files by the
   * Maven Performance plugin
   * </p>
   *
   * @param performanceReportWorkspaceName
   * @return the name of the PerformanceReport in the Build
   */
  public static String getPerformanceReportBuildFileName(String performanceReportWorkspaceName) {
    String result = performanceReportWorkspaceName;
    if (performanceReportWorkspaceName != null) {
      Pattern p = Pattern.compile("-[0-9]*\\.xml");
      Matcher matcher = p.matcher(performanceReportWorkspaceName);
      if (matcher.find()) {
        result = matcher.replaceAll(".xml");
      }
    }
    return result;
  }

  /**
   * look for performance reports based in the configured parameter includes.
   * 'includes' is - an Ant-style pattern - a list of files and folders
   * separated by the characters ;:,
   */
  public static List<FilePath> locatePerformanceReports(FilePath workspace, String includes) throws IOException, InterruptedException {

    // First use ant-style pattern
    /*
      try {
      FilePath[] ret = workspace.list(includes);
      if (ret.length > 0) {
        return Arrays.asList(ret);
      }
    */
    //Agoley : Possible fix, if we specify more than one result file pattern
    try {
      String parts[] = includes.split("\\s*[;:,]+\\s*");


      List<FilePath> files = new ArrayList<FilePath>();
      for (String path : parts) {
        FilePath[] ret = workspace.list(path);
        if (ret.length > 0) {
          files.addAll(Arrays.asList(ret));
        }
      }
      if (!files.isEmpty()) return files;

    } catch (IOException e) {
    }

    //Agoley:  seems like this block doesn't work
    // If it fails, do a legacy search
    ArrayList<FilePath> files = new ArrayList<FilePath>();
    String parts[] = includes.split("\\s*[;:,]+\\s*");
    for (String path : parts) {
      FilePath src = workspace.child(path);
      if (src.exists()) {
        if (src.isDirectory()) {
          files.addAll(Arrays.asList(src.list("**/*")));
        } else {
          files.add(src);
        }
      }
    }
    if (!files.isEmpty()) {
      return files;
    }

    //give up and just try direct matching on string
    File directFile = new File(includes);
    if (directFile.exists()) {
      files.add(new FilePath(directFile));
    }
    return files;
  }

  public static File[] getPerformanceReportsFromBuildDirectory(AbstractBuild<?, ?> build, String parserDisplayName) {
    File folder = new File(build.getRootDir() + "/" + PerformanceReportMap.getPerformanceReportFileRelativePath(parserDisplayName, ""));
    if (folder.listFiles() != null) {
      return folder.listFiles();
    }
    return new File[0];
  }

}
