# %%
from helpers import load_value
import pandas as pd
import seaborn as sns
import matplotlib

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
# %%
ns_5min = "simpleNetwork_5min_100Nodes_100MB"

history_size_options = range(1, 5 + 1)
dow_options = ["[1]", "[1, 2]", "[1, 2, 7]"]
#tod_options = ["[1]", "[1, 4]", "[1, 4, 12]", "[1, 4, 24]"]
tod_options = ["[1]", "[1, 4]", "[1, 4, 24]"]

base_fix = "0.9_PT24H_true"

# %%

FONT_SIZE = 14

x = []

for history_size in history_size_options:
    for dow in dow_options:
        for tod in tod_options:
            a = "Alg012_(%d_%s_%s)_(%s)" % (history_size, dow, tod, base_fix)
            d = {
                "hist": history_size,
                "$\it{Day of Week}$ Split": dow,
                "$\it{Time of Day}$ Split": tod,
                "% Availability": load_value(ns_5min, a, "Availability"),
                "% Excess Data": load_value(ns_5min, a, "ExcessData"),
            }
            x.append(d)

#x_df = pd.DataFrame(x).set_index("name")
x_df = pd.DataFrame(x)

# plot = x_df.plot(
#     #xticks=history_size_options,
#     x="hist",
#     y="% Availability",
#     subplots=True,
#     sharex=True,
#     fontsize=FONT_SIZE,
# )
# #plot.legend(loc="right", prop={'size': FONT_SIZE})
# plt.xlabel("size of history", fontsize=FONT_SIZE)
# plt.show()
#plot.get_figure().savefig('./eval-out/eval007_01.pdf', format='pdf', bbox_inches='tight')

# %%
g1 = sns.lineplot(data=x_df, x="hist", y="% Availability", style="$\it{Day of Week}$ Split", hue="$\it{Time of Day}$ Split")
g1.set_xticks(history_size_options)
g1.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0., prop={'size': FONT_SIZE})
g1.set_xlabel("size of history", fontsize=FONT_SIZE)
g1.set_ylabel("% Availability", fontsize=FONT_SIZE)
g1.tick_params(labelsize=FONT_SIZE)
g1.get_figure().savefig('./eval-out/eval016_01.pdf', format='pdf', bbox_inches='tight')
g1.get_figure().clf()

g2 = sns.lineplot(data=x_df, x="hist", y="% Excess Data", style="$\it{Day of Week}$ Split", hue="$\it{Time of Day}$ Split")
g2.set_xticks(history_size_options)
g2.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0., prop={'size': FONT_SIZE})
g2.set_xlabel("size of history", fontsize=FONT_SIZE)
g2.set_ylabel("% Excess Data", fontsize=FONT_SIZE)
g2.tick_params(labelsize=FONT_SIZE)
g2.get_figure().savefig('./eval-out/eval016_02.pdf', format='pdf', bbox_inches='tight')
g2.get_figure().clf()