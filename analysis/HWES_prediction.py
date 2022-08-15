
import datetime
import pickle
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from statsmodels.tsa.api import ExponentialSmoothing

from nodeGrid import Location, getNodeGrid
node_grid = getNodeGrid(100)

def holt_win_sea(y, y_to_train, y_to_test, seasonal_period, predict_date):
    # TO-DO Find the source
    fit1 = ExponentialSmoothing(y_to_train, seasonal_periods=seasonal_period, trend='add', seasonal='add', use_boxcox=True).fit()
    fcast1 = fit1.forecast(predict_date)
    plt.plot(fit1.fittedvalues, color='red', label='Training Data')
    s = pd.Series(fcast1)
    s2 = s.rolling(30).mean().to_numpy()
    plt.plot(s2, color='green', label='Predicted Time Series')
    plt.ylabel('Duration (s)')
    #plt.title('Additive trend and seasonal')
    plt.legend()
    plt.show()


with open("geolife_data_transformed/000.pkl", "rb") as f:
            data = pickle.load(f)

training_data = dict()
training_data_all = list()
test_data = dict()
node_change_total_time = 0.0
node_id_oldest = None
training_lenght = int(0.75*len(data["locations"]))
for i in range(1,len(data["locations"])-1):
    duration: datetime.timedelta = data["locations"][i+1]["time"] - data["locations"][i]["time"]
    if duration.total_seconds() > 180:
        continue
    node_id_old = node_grid.getClosestNodeID(Location(data["locations"][i]["lat"],float(data["locations"][i]["long"])))
    node_id_new = node_grid.getClosestNodeID(Location(data["locations"][i+1]["lat"],float(data["locations"][i+1]["long"])))
    if node_id_old != node_id_new:
        if  node_id_oldest is not None:
            training_data_all.append({'starting_date': data["locations"][i]["time"], 'duration': float(node_change_total_time)})
            id = node_id_oldest + "," + node_id_old
            if id not in training_data:
                training_data[id] = []
            if id not in test_data:
                test_data[id] = []
            #training_data[id].append({'duration': node_change_total_time, 'starting_date': data["locations"][i]["time"].hour * 60 + data["locations"][i]["time"].second})
            #day_of_the_week = data["locations"][i]["time"].toordinal()%7
            if i < training_lenght:
                training_data[id].append({'duration': node_change_total_time, 'starting_date': data["locations"][i]["time"]})
            else:
                test_data[id].append({'duration': node_change_total_time, 'starting_date': data["locations"][i]["time"]})
        node_change_total_time = 0
        node_id_oldest = node_id_old
    else:
        node_change_total_time += duration.total_seconds()


y = [o["duration"] for o in training_data["6-3,5-3"]]
x = [o["starting_date"] for o in training_data["6-3,5-3"]]
ty = [o["duration"] for o in test_data["6-3,5-3"]]
tx = [o["starting_date"] for o in test_data["6-3,5-3"]]

x2 = [o["starting_date"] for o in training_data_all]
y2 =  [o["duration"] for o in training_data_all]

df: pd.DataFrame = pd.DataFrame(training_data_all, columns=["starting_date", "duration"])
df_train = df.head(int(len(df)*0.7))
df_test = df.tail(int(len(df)*0.3))
    
predict_date = 1000
pd.plotting.register_matplotlib_converters()
print(df)
df["starting_date"] = pd.to_numeric(pd.to_datetime(df["starting_date"], format= "%Y-%m-%d"))
df_train["starting_date"] = pd.to_numeric(pd.to_datetime(df_train["starting_date"], format= "%Y-%m-%d"))
df_test["starting_date"] = pd.to_numeric(pd.to_datetime(df_test["starting_date"], format= "%Y-%m-%d"))
print(df)
print(df.dtypes)
dtype = [("starting_date", "int"), ("duration", "float")]
dt_z = [i for i in np.asarray(df["duration"]) if i != 0.0]
dt_tz = [i for i in np.asarray(df_train["duration"]) if i != 0.0]
dt_ttz =[i for i in np.asarray(df_test["duration"]) if i != 0.0] 
holt_win_sea(dt_z, dt_tz, dt_ttz, 234, predict_date)

ok = 1
