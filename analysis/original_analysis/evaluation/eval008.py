from helpers import load_value
import pandas as pd

# %%

ns_options = [3 ** 4, 4 ** 4, 5 ** 4]

# %%

x = []

for ns in ns_options:
    a_1 = "Alg001"
    a_12_24 = "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT24H_true)"

    ns_name = "complexNetwork_%dNodes_normalBandwidth_1GB" % ns

    d = {
        "Number of Nodes": ns,

        "Availability Baseline": load_value(ns_name, a_1, "Availability"),
        # "ExcessData_1": load_value(ns_name, a_1, "ExcessData"),

        "Availability FOMM": load_value(ns_name, a_12_24, "Availability"),
        "Excess Data FOMM": load_value(ns_name, a_12_24, "ExcessData"),
    }
    x.append(d)

x_df = pd.DataFrame(x)
# %%

# shows algorithms also work on complex network with different node densities
# table
print(x_df.to_latex(index=False, float_format="%.2f %%"))
