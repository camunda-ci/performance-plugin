package hudson.plugins.performance.parser.JMeterCsvParser

import lib.FormTagLib

def f = namespace(FormTagLib)

f.entry(title: _("Report files"), field:"glob") {
  f.textbox()
}
f.entry(title: _("csv.pattern"), field:"pattern", description: _("csv.pattern.description")) {
  f.textbox(default: _("csv.pattern.default"))
}
f.entry(title: _("csv.pattern.delimiter"), field:"delimiter") {
  f.textbox(default: _("csv.pattern.delimiter.default"))
}
f.entry(title: _("csv.skipFirstLine"), field:"skipFirstLine", description: _("csv.skipFirstLine.description")) {
  f.checkbox(default: true)
}
