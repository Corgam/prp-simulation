
import datetime
import imp
import pickle
import matplotlib.pyplot as plt
import numpy as np
from sklearn.linear_model import LinearRegression
from scipy import stats
import pandas as pd
import statsmodels.tsa.arima.model

import statsmodels.api as sm
from statsmodels.api import OLS

from nodeGrid import Location, getNodeGrid
node_grid = getNodeGrid(100)

with open("000.pkl", "rb") as f:
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
            training_data_all.append({'duration': node_change_total_time, 'starting_date': data["locations"][i]["time"]})
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

df: pd.DataFrame = pd.DataFrame(training_data_all)
# df["duration"].plot()
# df['duration'].rolling(window =10).mean().plot()
# df["duration"].diff().plot()


# plt.figure(figsize=(16,5), dpi=100)
# plt.plot(x2, y2, color='tab:red')
# plt.gca().set(title="test", xlabel="date", ylabel="dur")
# plt.show()
# ok = 1




def analyze_stationarity(timeseries, title):
    fig, ax = plt.subplots(2, 1, figsize=(16, 8))

    rolmean = pd.Series(timeseries).rolling(window=30).mean() 
    rolstd = pd.Series(timeseries).rolling(window=30).std()
    ax[0].plot(timeseries, label= title)
    ax[0].plot(rolmean, label='rolling mean')
    ax[0].plot(rolstd, label='rolling std (x10)')
    ax[0].set_title('30-day window')
    ax[0].legend()
    av = sum(timeseries)/len(timeseries)
    ax[0].axhline(y = av, color = "r")
    
    rolmean = pd.Series(timeseries).rolling(window=365).mean() 
    rolstd = pd.Series(timeseries).rolling(window=365).std()
    ax[1].plot(timeseries, label= title)
    ax[1].plot(rolmean, label='rolling mean')
    ax[1].plot(rolstd, label='rolling std (x10)')
    ax[1].set_title('365-day window')
    
pd.options.display.float_format = '{:.8f}'.format
analyze_stationarity(df['duration'], 'raw data')

# slope, intercept, r, p, std_err = stats.linregress(x, y)
# def myfunc(x):
#   return slope * x + intercept
# mymodel = list(map(myfunc, x))
# plt.scatter(x,y)
# av = sum(y)/len(y)
# plt.axhline(y = av, color = "r")
# plt.plot(x, mymodel)
# plt.show()

# x = sm.add_constant(x)
# model = OLS(y, x)
# res = model.fit()
# print(res.summary())



ok = 1
