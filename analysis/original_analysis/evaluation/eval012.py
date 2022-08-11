from helpers import load_value
import pandas as pd

# %%

ns_names = [
    "simpleNetwork_5min_100Nodes_100MB",
    "simpleNetwork_5min_400Nodes_100MB",
    "complexNetwork_81Nodes_normalBandwidth_1GB",
    "complexNetwork_625Nodes_normalBandwidth_1GB",
]

alg_names = [
    "Alg001",
    "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT5M_true)",
    "Alg011_true_false_0.5_PT10M",
    "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M",
]

alg_name_alias = {
    "Alg001": "Baseline",
    "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT5M_true)": "FOMM",
    "Alg011_true_false_0.5_PT10M": "Short Pause: fixed 10 minutes",
    "Alg012_(5_[1, 2, 7]_[1, 4, 24])_(0.9_PT5M_true)_Alg011_true_false_0.5_PT10M": "Combination",
}

ns_name_alias = {
    "simpleNetwork_5min_100Nodes_100MB": "Simple Network 100 Nodes",
    "simpleNetwork_5min_400Nodes_100MB": "Simple Network 400 Nodes",
    "complexNetwork_81Nodes_normalBandwidth_1GB": "Complex Network 81 Nodes",
    "complexNetwork_625Nodes_normalBandwidth_1GB": "Complex Network 625 Nodes",
}

# %%

print("""
\\begin{table}%
  \centering
""")

for ns in ns_names:
    x = []
    for alg in alg_names:
        x.append({
            "Algorithm": alg_name_alias[alg],

            "Availability": load_value(ns, alg, "Availability"),
            "Excess Data": load_value(ns, alg, "ExcessData"),
        })

    x_df = pd.DataFrame(x)
    # print(ns)
    # print(x_df)
    print("\subfloat[" + ns_name_alias[ns] + "]{")
    print(x_df.to_latex(index=False, float_format="%.2f %%"))
    print("""
    }%
  \qquad
    """)

print("""
\caption{Combinations of Algorithms}%
  \label{tab:eval_combination}%
\end{table}
""")

# %%

# present as 4 tables (or 1 table with rows=algs, columns Availability/ExcessData, combined column ns name)
