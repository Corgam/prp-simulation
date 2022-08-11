from helpers import load_value
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42

# %%

ns_5min = "simpleNetwork_5min_100Nodes_100MB"

history_size_options = range(1, 5 + 1)

x = []

for i in history_size_options:
    d = {
        "i": i,
        "Alg003_Time": load_value(ns_5min, "Alg003_%d_true_(1.0_PT24H_false)" % i, "Availability"),
        "Alg003_WrongTime": load_value(ns_5min, "Alg003_%d_true_(1.0_PT24H_false)" % i, "ExcessData"),
        "Alg003_RAM": load_value(ns_5min, "Alg003_%d_true_(1.0_PT24H_false)" % i, "RAMFootprint", times_100=False) / 1024,

        "Alg003_woReset_Time": load_value(ns_5min, "Alg003_%d_false_(1.0_PT24H_false)" % i, "Availability"),
        "Alg003_woReset_WrongTime": load_value(ns_5min, "Alg003_%d_false_(1.0_PT24H_false)" % i, "ExcessData"),
        "Alg003_woReset_RAM": load_value(ns_5min, "Alg003_%d_false_(1.0_PT24H_false)" % i, "RAMFootprint",
                                         times_100=False) / 1024,

        "Alg004_Time": load_value(ns_5min, "Alg004_%d_true_(1.0_PT24H_false)" % i, "Availability"),
        "Alg004_WrongTime": load_value(ns_5min, "Alg004_%d_true_(1.0_PT24H_false)" % i, "ExcessData"),
        "Alg004_RAM": load_value(ns_5min, "Alg004_%d_true_(1.0_PT24H_false)" % i, "RAMFootprint",
                                 times_100=False) / 1024,

        "Alg004_woReset_Time": load_value(ns_5min, "Alg004_%d_false_(1.0_PT24H_false)" % i, "Availability"),
        "Alg004_woReset_WrongTime": load_value(ns_5min, "Alg004_%d_false_(1.0_PT24H_false)" % i, "ExcessData"),
        "Alg004_woReset_RAM": load_value(ns_5min, "Alg004_%d_false_(1.0_PT24H_false)" % i, "RAMFootprint",
                                         times_100=False) / 1024,

        "Alg001_Time": load_value(ns_5min, "Alg001", "Availability"),
        "Alg001_WrongTime": load_value(ns_5min, "Alg001", "ExcessData"),
        "Alg001_RAM": load_value(ns_5min, "Alg001", "RAMFootprint", times_100=False) / 1024,
    }
    x.append(d)

x_df = pd.DataFrame(x).set_index("i", drop=False)

# %%

plot = x_df.plot(
    y=["Alg003_Time", "Alg003_woReset_Time", "Alg004_Time", "Alg004_woReset_Time", "Alg001_Time"],
    xticks=history_size_options,
    xlabel="size of history",
    ylabel="% Availability"
)
plot.legend(
    ["MOMM w/ reset", "MOMM w/o reset", "VOMM w/ reset", "VOMM w/o reset", "Baseline"],
    loc=(0.03, 0.1,)
)
plt.show()
plot.get_figure().savefig('./eval-out/eval001_02.pdf', format='pdf', bbox_inches='tight')

plot = x_df.plot(
    y=["Alg003_WrongTime", "Alg003_woReset_WrongTime", "Alg004_WrongTime", "Alg004_woReset_WrongTime",
       "Alg001_WrongTime"],
    xticks=history_size_options,
    xlabel="size of history",
    ylabel="% Excess Data"
)
plot.legend(
    ["MOMM w/ reset", "MOMM w/o reset", "VOMM w/ reset", "VOMM w/o reset", "Baseline"],
    loc=(0.03, 0.1,)
)
plt.show()
plot.get_figure().savefig('./eval-out/eval001_03.pdf', format='pdf', bbox_inches='tight')

'''
plot = x_df.plot(
    y=["Alg003_RAM", "Alg003_woReset_RAM", "Alg004_RAM", "Alg004_woReset_RAM",
       "Alg001_RAM"],
    xticks=history_size_options,
    xlabel="size of history",
    ylabel="RAM usage in KB"
)
plot.legend(
    ["MOMM w/ reset", "MOMM w/o reset", "VOMM w/ reset", "VOMM w/o reset", "Baseline"],
    # loc=(0.03, 0.1,)
)
plt.show()
plot.get_figure().savefig('./eval-out/eval001_04.pdf', format='pdf', bbox_inches='tight')
'''