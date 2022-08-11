from helpers import load_value
import pandas as pd

# %%

ns_5min = "simpleNetwork_5min_100Nodes_100MB"

preload_buffer_options = list(["PT0S", "PT1M", "PT5M", "PT10M", "PT24H"])

x = []

for preload_buffer in preload_buffer_options:
    x.append({
        "topN": 0.9,
        "Preload Buffer": preload_buffer,
        "Availability": load_value(ns_5min, "Alg004_2_true_(0.9_%s_true)" % preload_buffer, "Availability"),
        "ExcessData": load_value(ns_5min, "Alg004_2_true_(0.9_%s_true)" % preload_buffer, "ExcessData"),
    })
    x.append({
        "topN": 0.95,
        "Preload Buffer": preload_buffer,
        "Availability": load_value(ns_5min, "Alg004_2_true_(0.95_%s_true)" % preload_buffer, "Availability"),
        "ExcessData": load_value(ns_5min, "Alg004_2_true_(0.95_%s_true)" % preload_buffer, "ExcessData"),
    })

x_df = pd.DataFrame(x)
x_df = x_df[x_df["topN"] == 0.9]

# %%

print(x_df.to_latex(float_format="%.2f %%", columns=["Preload Buffer", "Availability", "ExcessData"], index=False))
