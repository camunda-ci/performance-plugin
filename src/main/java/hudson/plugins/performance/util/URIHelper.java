package hudson.plugins.performance.util;

public class URIHelper {

  public static String asStaplerURI(String uri) {
    return uri.replace("http:", "").replaceAll("/", "_");
  }

}
