
import datetime
import pickle
from statistics import mean
import matplotlib.pyplot as plt

from nodeGrid import Location, getNodeGrid
node_grid = getNodeGrid(100)

with open("geolife_data_transformed/000.pkl", "rb") as f:
            data = pickle.load(f)

training_data_all = list()
test_data = dict()
node_change_total_time = 0.0
node_id_oldest = None
for i in range(1,len(data["locations"])-1):
    duration: datetime.timedelta = data["locations"][i+1]["time"] - data["locations"][i]["time"]
    if duration.total_seconds() > 180:
        continue
    node_id_old = node_grid.getClosestNodeID(Location(data["locations"][i]["lat"],float(data["locations"][i]["long"])))
    node_id_new = node_grid.getClosestNodeID(Location(data["locations"][i+1]["lat"],float(data["locations"][i+1]["long"])))
    if node_id_old != node_id_new:
        if  node_id_oldest is not None:
            training_data_all.append({'dayOfTheWeek': data["locations"][i]["time"].toordinal()%7, 'duration': float(node_change_total_time)})
            id = node_id_oldest + "," + node_id_old
        node_change_total_time = 0
        node_id_oldest = node_id_old
    else:
        node_change_total_time += duration.total_seconds()


x = [item["dayOfTheWeek"] for item in training_data_all]
y = [item["duration"] for item in training_data_all]

x_averages = [[],[],[],[],[],[],[]]
for item in training_data_all:
    day = item["dayOfTheWeek"]
    x_averages[day].append(item["duration"])

average_points_x = []
average_points_y = []
for av in x_averages:
    if len(av) != 0:
        average_points_x.append(x_averages.index(av))
        average_points_y.append(mean(av))


plt.scatter(x,y, label="Durations Data")
av = sum(y)/len(y)
plt.axhline(y = av, color = "r", label= "Arithmetic Mean")
plt.ylabel("Duration (s)")
plt.scatter(average_points_x, average_points_y, color = "r", label= "Day's Mean")
plt.legend(fontsize=10, title_fontsize=15)
plt.show()
debug_stop = 1
