package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Predicate;
import com.google.common.html.HtmlEscapers;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.util.UpdateJson;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.*;

public class CheckIdeHtmlReportBuilder {

  @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
  public static void build(@NotNull File report,
                           @NotNull String ideVersion,
                           @NotNull Predicate<UpdateJson> updateFilter,
                           @NotNull Map<UpdateJson, ProblemSet> results)
    throws IOException {
    Map<String, Map<UpdateJson, ProblemSet>> pluginsMap = new TreeMap<String, Map<UpdateJson, ProblemSet>>();

    for (Map.Entry<UpdateJson, ProblemSet> entry : results.entrySet()) {
      Map<UpdateJson, ProblemSet> pluginMap = pluginsMap.get(entry.getKey().getPluginId());
      if (pluginMap == null) {
        pluginMap = new HashMap<UpdateJson, ProblemSet>();
        pluginsMap.put(entry.getKey().getPluginId(), pluginMap);
      }

      pluginMap.put(entry.getKey(), entry.getValue());
    }

    PrintWriter out = new PrintWriter(report);

    try {
      out.append("<html>\n" +
                 "<head>\n" +
                 "  <title>Report created at " + DateFormat.getDateTimeInstance().format(new Date()) + "</title>\n" +
                 "\n" +
                 "  <link rel=\"stylesheet\" href=\"http://code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css\">\n" +
                 "  <script src=\"http://code.jquery.com/jquery-1.9.1.js\"></script>\n" +
                 "  <script src=\"http://code.jquery.com/ui/1.10.4/jquery-ui.js\"></script>\n" +

                 "  <style type=\"text/css\">\n" +
                 "    .errorDetails {\n" +
                 "      padding: 2px;\n" +
                 "    }\n" +
                 "" +
                 "    .updates div div:nth-child(odd) {\n" +
                 "      background: #eee;\n" +
                 "    }\n" +
                 "" +
                 "    .marker {\n" +
                 "      white-space: pre;\n" +
                 "      font-weight: bold;\n" +
                 "    }\n" +
                 "\n" +
                 "    .ok .marker {\n" +
                 "      background: #0f0;\n" +
                 "      color: #0f0;\n" +
                 "    }\n" +
                 "    .hasError .marker {\n" +
                 "      background: #f00;\n" +
                 "      color: #f00;\n" +
                 "    }\n" +
                 "" +
                 "    .errorDetails a {\n" +
                 "      color: #2B587A !important;\n" +
                 "    }\n" +
                 "" +
                 "    .errLoc {\n" +
                 "      display: none;\n" +
                 "      margin-left: 100px;\n" +
                 "      padding: 2px;\n" +
                 "    }\n" +
                 "" +
                 "    .excluded .marker {\n" +
                 "      background: #888 !important;\n" +
                 "    }" +

                 "  </style>\n" +

                 "</head>\n" +
                 "\n" +
                 "<body>\n" +
                 "\n" +
                 "<div id=\"tabs\">\n");

      if (pluginsMap.isEmpty()) {
        out.print("No plugins to check.\n");
      }
      else {
        out.print("  <ul>\n");

        int idx = 1;
        for (String pluginId : pluginsMap.keySet()) {
          out.printf("    <li><a href=\"#tabs-%d\">%s</a></li>\n", idx++, pluginId);
        }

        out.append("  </ul>\n");

        idx = 1;
        for (Map.Entry<String, Map<UpdateJson, ProblemSet>> entry : pluginsMap.entrySet()) {
          out.printf("  <div id=\"tabs-%d\">\n", idx++);

          if (entry.getValue().isEmpty()) {
            out.printf("There are no updates compatible with %s in the Plugin Repository\n", ideVersion);
          }
          else {
            List<UpdateJson> updates = new ArrayList<UpdateJson>(entry.getValue().keySet());
            Collections.sort(updates, Collections.reverseOrder(new UpdatesComparator()));

            for (UpdateJson update : updates) {
              ProblemSet problems = entry.getValue().get(update);

              out.printf("<div class=\"updates\">\n");

              out.printf("  <h3 class='%s %s'><span class='marker'>   </span> %s (#%d) %s</h3>\n",
                         problems.isEmpty() ? "ok" : "hasError",
                         updateFilter.apply(update) ? "" : "excluded",
                         HtmlEscapers.htmlEscaper().escape(update.getVersion()),
                         update.getUpdateId(),
                         problems.isEmpty() ? "" : "<small>" + problems.count() + " errors found</small>"
              );

              out.printf("  <div>\n");

              if (problems.isEmpty()) {
                out.printf(" No problems.");
              }
              else {
                List<Problem> problemList = new ArrayList<Problem>(problems.getAllProblems());
                Collections.sort(problemList, new ToStringProblemComparator());

                for (Problem problem : problemList) {
                  out.append("    <div class='errorDetails'>").append(HtmlEscapers.htmlEscaper().escape(problem.getDescription())).append(' ')
                    .append("<a href=\"#\" class='detailsLink'>details</a>\n");


                  out.append("<div class='errLoc'>");

                  List<ProblemLocation> locationList = new ArrayList<ProblemLocation>(problems.getLocations(problem));
                  Collections.sort(locationList, new ToStringCachedComparator<ProblemLocation>());

                  boolean isFirst = true;
                  for (ProblemLocation location : locationList) {
                    if (isFirst) {
                      isFirst = false;
                    }
                    else {
                      out.append("<br>");
                    }

                    out.append(location.toString());
                  }

                  out.append("</div></div>");
                }
              }

              out.printf("  </div>\n");
              out.printf("</div>\n");
            }
          }

          out.append("  </div>\n");
        }
      }

      out.append("</div>\n"); // tabs


      InputStream reportScript = CheckIdeHtmlReportBuilder.class.getResourceAsStream("/reportScript.js");
      out.append("<script>\n");
      IOUtils.copy(reportScript, out);
      out.append("</script>\n");

      out.append("</body>\n");
      out.append("</html>");
    }
    finally {
      out.close();
    }
  }

  private static class ToStringProblemComparator extends ToStringCachedComparator<Problem> {
    @NotNull
    @Override
    protected String toString(Problem object) {
      return object.getDescription();
    }
  }

  private static class ToStringCachedComparator<T> implements Comparator<T> {

    private final IdentityHashMap<T, String> myCache = new IdentityHashMap<T, String>();

    @NotNull
    protected String toString(T object) {
      return object.toString();
    }

    @NotNull
    private String getDescriptor(T obj) {
      String res = myCache.get(obj);
      if (res == null) {
        res = toString(obj);
        myCache.put(obj, res);
      }

      return res;
    }

    @Override
    public int compare(T o1, T o2) {
      return getDescriptor(o1).compareTo(getDescriptor(o2));
    }
  }

  private static class UpdatesComparator implements Comparator<UpdateJson> {
    @Override
    public int compare(UpdateJson o1, UpdateJson o2) {
      return o1.getUpdateId() - o2.getUpdateId();
    }
  }
}