
import datetime
import pickle
from turtle import color
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import pandas

from nodeGrid import Location, getNodeGrid
node_grid = getNodeGrid(100)

def dayValue2String(value: int):
    match value:
        case 0:
            return "Sunday"
        case 1:
            return "Monday"   
        case 2:
            return "Tuesday"
        case 3:
            return "Wednesday"   
        case 4:
            return "Thursday"
        case 5:
            return "Friday"
        case 6:
            return "Saturday"  

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
            training_data_all.append({'dayOfTheWeek': dayValue2String(data["locations"][i]["time"].toordinal()%7), 'duration': float(node_change_total_time)})
            id = node_id_oldest + "," + node_id_old
        node_change_total_time = 0
        node_id_oldest = node_id_old
    else:
        node_change_total_time += duration.total_seconds()


x2 = [item["dayOfTheWeek"] for item in training_data_all]
y = [item["duration"] for item in training_data_all]

data = list(map(list, list(zip(x2,y))))
df = pandas.DataFrame(data, columns=["Day","duration"])
df.head()
df = df[df.duration < df.duration.quantile(0.99)]
sns.ecdfplot(df, x="duration", hue="Day")
av = df["duration"].mean()
plt.axvline(x = av, color = "r", label= "Arithmetic Mean")
plt.annotate("Total arithmetic mean",xytext=(av+300,0.2),xy=(av,0.2), color="red",arrowprops={"arrowstyle":"->", "color": "red"})
plt.yticks(np.arange(0,1,0.1))
plt.xticks(np.arange(0,10000,500))
plt.xlabel("Duration (s)")
plt.show()
debug_stop = 1
