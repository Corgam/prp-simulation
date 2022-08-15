import matplotlib.pyplot as plt
from original_analysis.evaluation.helpers import plot_pareto_frontier

availability = [75.47,73.31,73.00,72.90,72.84,72.57,71.87,70.66,62.19,61.43,72.90,72.92]
x_all = [75.47,75.04,74.64,74.28,74.16,73.93,73.31,73.24,73.17,73.05,73.00,72.94,72.90,72.90,72.84,72.74,72.57,72.36,71.89,71.87,71.77,71.12,70.66,62.19,61.43]
excess_data = [71.68,66.63,65.56,65.44,61.58,63.15,49.61,57.84,23.40,0.00,65.31,65.40]
y_all = [71.68,70.89,70.07,69.19,68.97,68.42,66.63,66.33,66.05,65.83,65.56,65.49,65.31,65.44,61.58,64.95,63.15,64.01,62.69,49.61,62.33,60.22,57.84,23.40,0.00]
labels = ["T-FOMM (0 pctl)","T-FOMM (Median)","T-FOMM (DA, daysOfWeek)","FOMM","T-FOMM (HWES, node)","T-FOMM (HWES, discretization)","T-FOMM (HWES, user)","T-FOMM (100 pctl)","VOMM","Keep-on-closest"]

plot = plot_pareto_frontier(
    availability, excess_data, y_desc=True,
    labelX="% Availability",
    labelY="% Excess Data",
)
plt.show()

debug_stop = 1
