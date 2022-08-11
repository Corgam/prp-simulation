from helpers import load_value
import pandas as pd

# %%

buffer_duration_options = ["PT24H", "PT5M", "PT0S"]

ns_name = "complexNetwork_81Nodes_normalBandwidth_1GB"

# %%

x = []

for bd in buffer_duration_options:
    a_12 = "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_%s_true)" % bd

    d = {
        "bd": bd,

        "Availability": load_value(ns_name, a_12, "Availability"),
        "ExcessData": load_value(ns_name, a_12, "ExcessData"),
    }
    x.append(d)

d = {
    "bd": "Alg001",

    "Availability": load_value(ns_name, "Alg001", "Availability"),
    "ExcessData": load_value(ns_name, "Alg001", "ExcessData"),
}
x.append(d)

x_df = pd.DataFrame(x)

# %%

# shows that alg 12 on complex network also works with duration buffer extension
# a little bit worse than on simple network because transfer time estimate is more complex and not as accurate
