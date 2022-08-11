import matplotlib.pyplot as plt
from os import listdir, getcwd

STATS_DIR = getcwd() + "/../stats-out/"

def get_algs(ns_name, alg_prefix=""):
    algs = []
    for f in listdir(STATS_DIR + ns_name + "/"):
        if f.startswith(alg_prefix):
            algs.append(f)
    return algs


def load_value(ns_name, alg_name, metric_name, times_100=True):
    f = open(STATS_DIR + ns_name + "/" + alg_name + "/" + metric_name + ".txt", "r")
    return float(f.read()) * (100 ** times_100)


def pareto_front(Xs, Ys, x_desc=False, y_desc=False, labels=None):
    sorted_list = sorted([[((-1) ** x_desc) * Xs[i], ((-1) ** y_desc) * Ys[i], labels[i]] for i in range(len(Xs))],
                         reverse=True)
    pareto_front = [sorted_list[0]]
    for pair in sorted_list[1:]:
        if pair[1] >= pareto_front[-1][1]:
            # pareto_front.append([((-1) ** x_desc) * pair[0], ((-1) ** y_desc) * pair[1], pair[2]])
            pareto_front.append(pair)

    pareto_front = map(lambda x: [((-1) ** x_desc) * x[0], ((-1) ** y_desc) * x[1], x[2]], pareto_front)

    return pareto_front


# https://sirinnes.wordpress.com/2013/04/25/pareto-frontier-graphic-via-python/
def plot_pareto_frontier(Xs, Ys, x_desc=False, y_desc=False, labels=None,  labelX="", labelY=""):
    sorted_list = sorted([[((-1) ** x_desc) * Xs[i], ((-1) ** y_desc) * Ys[i]] for i in range(len(Xs))], reverse=True)
    pareto_front = [sorted_list[0]]
    for pair in sorted_list[1:]:
        if pair[1] >= pareto_front[-1][1]:
            pareto_front.append(pair)

    plot = plt.scatter(Xs, Ys)

    texts = []
    if labels is not None:
        for (i, (l, p)) in enumerate(labels):
            texts.append(plt.annotate(l, (Xs[i], Ys[i]), textcoords='offset points', xytext=p))

    pf_X = [((-1) ** x_desc) * pair[0] for pair in pareto_front]
    pf_Y = [((-1) ** y_desc) * pair[1] for pair in pareto_front]
    plt.plot(pf_X, pf_Y)
    plt.xlabel(labelX)
    plt.ylabel(labelY)

    return plot
