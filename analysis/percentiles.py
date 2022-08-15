import matplotlib.pyplot as plt

availability = [75.47, 75.04,74.64,74.16,73.93,73.31,72.90,72.74,72.36,71.77,71.12,70.66]
excess_data = [71.68, 70.89,70.07,68.97,68.42,66.63,65.44,64.95,64.01,62.33,60.22,57.84]
labels = ["0 pctl", "10 pctl","20 pctl","30 pctl","40 pctl","50 pctl","FOMM","60 pctl","70 pctl","80 pctl","90 pctl"]

plt.scatter(availability, excess_data)

for i, txt in enumerate(labels):
    plt.annotate(txt, (availability[i], excess_data[i]),xytext=(availability[i] - 0.6, excess_data[i] - 0.2))

plt.annotate("100 pctl", (availability[11], excess_data[11]),xytext=(availability[11] + 0.1, excess_data[11] - 0.2))
plt.ylabel("% Excess Data")
plt.xlabel("% Availability")
plt.show()
debug_stop = 1
