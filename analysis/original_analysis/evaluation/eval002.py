from helpers import load_value, plot_pareto_frontier
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
# %%

ns_5min = "simpleNetwork_5min_100Nodes_100MB"

topN_options = list(["0.8", "0.9", "0.95", "0.98", "1.0", "2.0"])


def get_topN_label(topN):
    if float(topN) >= 1:
        return "fix topN of %d" % int(float(topN))
    else:
        return "dynamic topN of %d%%" % int(float(topN) * 100.0)


# %%

x = []

p = {
    "fix topN of 1 + EoT extension": (20, -7),
    "fix topN of 2 + EoT extension": (-150, 12),
    "dynamic topN of 80% + EoT extension": (-200, 0),
    "dynamic topN of 90% + EoT extension": (-195, 25),
    "dynamic topN of 95% + EoT extension": (-200, 2),
    "dynamic topN of 98% + EoT extension": (-200, 5),

    "fix topN of 1": (-70, 0),
    "fix topN of 2": (-70, -3),
    "dynamic topN of 80%": (-112, -8),
    "dynamic topN of 90%": (-112, 0),
    "dynamic topN of 95%": (-112, 0),
    "dynamic topN of 98%": (-112, 0),
}

for topN in topN_options:
    label = get_topN_label(topN) + " + EoT extension"
    x.append({
        "topN": float(topN),
        "EoT": True,
        "Time": load_value(ns_5min, "Alg004_2_true_(%s_PT24H_true)" % topN, "Availability"),
        "WrongTime": load_value(ns_5min, "Alg004_2_true_(%s_PT24H_true)" % topN, "ExcessData"),
        "Label": (label, p[label]),
    })
    label = get_topN_label(topN)
    x.append({
        "topN": float(topN),
        "EoT": False,
        "Time": load_value(ns_5min, "Alg004_2_true_(%s_PT24H_false)" % topN, "Availability"),
        "WrongTime": load_value(ns_5min, "Alg004_2_true_(%s_PT24H_false)" % topN, "ExcessData"),
        "Label": (label, p[label]),
    })

x_df = pd.DataFrame(x)

plot = plot_pareto_frontier(
    x_df["Time"], x_df["WrongTime"], y_desc=True, labels=x_df["Label"],
    labelX="% Availability",
    labelY="% Excess Data",
)

plt.arrow(70.27925, 69, 0, -10, head_length=3, head_width=0.1, length_includes_head=True)
plt.arrow(69.46494, 63, 0, -5, head_length=3, head_width=0.1, length_includes_head=True)

plt.show()

plot.get_figure().savefig('./eval-out/eval002.pdf', format='pdf', bbox_inches='tight')
