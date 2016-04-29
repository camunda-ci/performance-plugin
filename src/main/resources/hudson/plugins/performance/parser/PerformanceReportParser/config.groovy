package hudson.plugins.performance.parser.PerformanceReportParser

import lib.FormTagLib

def f = namespace(FormTagLib)

f.entry(title: _("Report files"), field: "glob") {
  f.textbox()
}

if (descriptor.getDisplayName().equals('JmeterSummarizer')) {
  f.entry(title: _("Summariser Date Format"), field: "logDateFormat") {
    f.textbox()
  }
}
