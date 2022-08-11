from helpers import load_value
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
# %%

x = []

history_size_options = range(1, 5 + 1)

ns_5min = "simpleNetwork_5min_100Nodes_100MB"

for i in history_size_options:
    d = {
        "i": i,

        "Alg004_Time": load_value(ns_5min, "Alg004_%d_true_(0.9_PT24H_true)" % i, "Availability"),
        "Alg004_WrongTime": load_value(ns_5min, "Alg004_%d_true_(0.9_PT24H_true)" % i, "ExcessData"),
    }
    x.append(d)

x_df = pd.DataFrame(x).set_index("i", drop=True)

# %%


plot = x_df[["Alg004_Time", "Alg004_WrongTime"]].plot(xticks=history_size_options, xlabel="size of history", )
plot.legend(["% Availability", "% Excess Data"])
plt.show()
plot.get_figure().savefig('./eval-out/eval005.pdf', format='pdf', bbox_inches='tight')
