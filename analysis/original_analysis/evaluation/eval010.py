import matplotlib.pyplot as plt
import numpy as np

from helpers import STATS_DIR
import pandas as pd
import matplotlib

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
# %%


x_df = pd.read_csv(STATS_DIR + "simpleNetwork_5min_100Nodes_100MB/Alg001/PauseTimeOverall.txt", header=None)

x_df = x_df / 60

x_df_sorted = x_df.sort_values(by=0)

# %%

plot = x_df_sorted.plot.hist(
    bins=np.linspace(0, 100, 50),
    histtype='step',
)
plt.xlabel("Pause Duration in Minutes")
plot.legend().remove()

plt.show()
plot.get_figure().savefig('./eval-out/eval010.pdf', format='pdf', bbox_inches='tight')

# %%

x_df_sorted.quantile(q=[0.1, 0.25, 0.5, 0.75, 0.9])
