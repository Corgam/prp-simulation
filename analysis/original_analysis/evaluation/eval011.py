from helpers import load_value
import pandas as pd

# %%

alg_options = [
    "Alg001",

    "Alg011_false_false_0.5_PT10M",
    "Alg011_false_false_0.5_PT30M",
    "Alg011_false_false_0.5_PT1H",

    "Alg011_false_true_0.5_PT10M",
    "Alg011_false_true_0.5_PT30M",
    "Alg011_false_true_0.5_PT1H",

    "Alg014_(PT25M_0.5)_(1_[1, 2, 7]_[1, 4, 24])",
    "Alg014_(PT30M_0.5)_(1_[1, 2, 7]_[1, 4, 24])",

    "Alg011_true_false_0.5_PT10M",
]

ns_name = "simpleNetwork_5min_100Nodes_100MB"

# %%

x = []

for alg in alg_options:
    d = {
        "alg": alg,

        "Availability": load_value(ns_name, alg, "Availability"),
        "Excess Data": load_value(ns_name, alg, "ExcessData"),
    }
    x.append(d)

x_df = pd.DataFrame(x)

q = [i == "Alg001" or i.startswith("Alg011_false") for i in x_df['alg']]
x_df_2 = x_df[q]

print(x_df_2.to_latex(index=False, float_format="%.2f %%"))
