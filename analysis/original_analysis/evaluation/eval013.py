from helpers import STATS_DIR
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
# %%

ns_name = "simpleNetwork_5min_100Nodes_100MB"

alg_names = [
    "Alg001",

    "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT5M_true)",
    "Alg011_true_false_0.5_PT10M",
    "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
]

# user = "overall"
user = "003.out"

x_df = None

for alg in alg_names:
    csv = pd.read_csv(STATS_DIR + ns_name + "/" + alg + "/AvailabilityOverTime/" + user + ".csv",
                      parse_dates=["time"])
    csv.set_index("time", inplace=True)
    csv["availability"] = csv["availability"] * 100
    csv.rename(columns={"availability": alg}, inplace=True)

    if x_df is None:
        x_df = csv
    else:
        x_df = x_df.merge(csv, left_index=True, right_index=True)

plot = x_df.plot(
    xlabel="Time",
    ylabel="% Availability"
)
plot.legend(
    ["Baseline", "FOMM", "10m short pause", "combination"]
)
plt.show()
plot.get_figure().savefig('./eval-out/eval013.pdf', format='pdf', bbox_inches='tight')
