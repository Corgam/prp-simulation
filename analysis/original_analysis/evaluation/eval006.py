from helpers import load_value, get_algs, pareto_front
import pandas as pd
import matplotlib

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
# %%

x = []

ns_5min = "simpleNetwork_5min_100Nodes_100MB"

for a in get_algs(ns_5min, ""):
    d = {
        "name": a,

        "Availability": load_value(ns_5min, a, "Availability"),
        "ExcessData": load_value(ns_5min, a, "ExcessData"),
    }
    x.append(d)

x_df = pd.DataFrame(x)


x_df_2 = pd.DataFrame(pareto_front(x_df["Availability"], x_df["ExcessData"], y_desc=True, labels=x_df["name"]))

# %%
