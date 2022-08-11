from helpers import load_value
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
# %%

ns_5min = "simpleNetwork_5min_100Nodes_100MB"

history_size_options = range(1, 5 + 1)
dow_options = ["[1]", "[1, 2]", "[1, 2, 7]"]
tod_options = ["[1]", "[1, 4]", "[1, 4, 12]", "[1, 4, 24]"]

base_fix = "0.9_PT24H_true"

history_size_fix = 5
dow_fix = "[1]"
tod_fix = "[1]"

# %%

FONT_SIZE = 14

x = []

for history_size in history_size_options:
    a = "Alg012_(%d_%s_%s)_(%s)" % (history_size, dow_fix, tod_fix, base_fix)
    d = {
        "name": history_size,

        "% Availability": load_value(ns_5min, a, "Availability"),
        "% Excess Data": load_value(ns_5min, a, "ExcessData"),
    }
    x.append(d)

x_df = pd.DataFrame(x).set_index("name")

plot = x_df.plot(
    xticks=history_size_options,
    fontsize=FONT_SIZE,
)
plot.legend(loc="right", prop={'size': FONT_SIZE})
plt.xlabel("size of history", fontsize=FONT_SIZE)
plt.show()
plot.get_figure().savefig('./eval-out/eval007_01.pdf', format='pdf', bbox_inches='tight')

x = []

for dow in dow_options:
    a = "Alg012_(%d_%s_%s)_(%s)" % (history_size_fix, dow, tod_fix, base_fix)
    d = {
        "name": dow,

        "% Availability": load_value(ns_5min, a, "Availability"),
        "% Excess Data": load_value(ns_5min, a, "ExcessData"),
    }
    x.append(d)

x_df = pd.DataFrame(x).set_index("name")

plot = x_df.plot(
    fontsize=FONT_SIZE,
)
plot.legend(loc="right", prop={'size': FONT_SIZE})
plt.xlabel("day of week options", fontsize=FONT_SIZE)
plt.show()
plot.get_figure().savefig('./eval-out/eval007_02.pdf', format='pdf', bbox_inches='tight')

x = []

for tod in tod_options:
    a = "Alg012_(%d_%s_%s)_(%s)" % (history_size_fix, dow_fix, tod, base_fix)
    d = {
        "name": tod,

        "% Availability": load_value(ns_5min, a, "Availability"),
        "% Excess Data": load_value(ns_5min, a, "ExcessData"),
    }
    x.append(d)

x_df = pd.DataFrame(x).set_index("name")

plot = x_df.plot(
    fontsize=FONT_SIZE,
)
plot.legend(loc="right", prop={'size': FONT_SIZE})
plt.xlabel("time of day options", fontsize=FONT_SIZE)
plt.show()
plot.get_figure().savefig('./eval-out/eval007_03.pdf', format='pdf', bbox_inches='tight')
