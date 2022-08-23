# Temporal Predictive Replica Placement in FogStore - A Simulation Framework

This simulation is a fork from the [original prp-simulation](https://github.com/MalteBellmann/prp-simulation) extending it by the temporal prediction, thus being able to simulate spatio-temporal prediction models.


## Structure:

- geolife-data: location of the raw Geolife Data
- geolife-data-transformed: populated by the `me.mbe.prp.TransformGeolife` main method in the test module
- src code
    - main: code of the framework, algorithms, etc.
        - algorithms
        - base: helper methods
        - core: framework
        - data
        - metrics
        - network
        - nodes
    - test: code for the evaluation
- stats-out: evaluation results saved here
- analysis: python files for analysis and plotting of the results

---

## Setup:

1. Clone repository
2. Setup gradle
3. Copy the [Geolife Data][1] into the geolife-data folder.
4. Run the `me.mbe.prp.TransformGeolife` main method in the test module
5. Run the evaluations in `me.mbe.prp.geolife.Evaluation`
6. Results can be found in the `stats-out` directory (The results folder already contains the results files, in order to create new ones, delete the existing ones.)

---

## Algorithms:

### Spatio-temporal algorithms (our proposed models):
- Baseline
    - `me.mbe.algorithms.Alg000`: Store data on all nodes at all times.
    - `me.mbe.algorithms.Alg001`: Store data only on closest node when application active.
    - `me.mbe.algorithms.nextnodepred.Alg004`: Variable Order Markov Model.
    - `me.mbe.algorithms.nextnodepred.Alg012`: Fusion Multi Order Markov Model.
- T-VOMM
    - `me.mbe.algorithms.nextnodepred.AlgT04`: Temporal Variable Order Markov Model: Not mentioned in the thesis
- T-FOMM
    - `me.mbe.algorithms.nextnodepred.AlgT012`: Temporal Fusion Multi Order Markov Model with multiple temporal models: Percentiles (PCTL), Temporal Discretization (TD) and [Holt Winter’s Exponential Smoothing (HWES)](https://github.com/nchandra/ExponentialSmoothing)

The not-finished complex network implementation can be found in the `complex_network` branch.

### Spatial-only algorithms (original thesis):

- Baseline
  - `me.mbe.algorithms.Alg000`: Store data on all nodes at all times.
  - `me.mbe.algorithms.Alg001`: Store data only on closest node when application active.
- Next Node Prediction
  - `me.mbe.algorithms.nextnodepred.Alg003`: (Multi Order) Markov Model.
  - `me.mbe.algorithms.nextnodepred.Alg004`: Variable Order Markov Model.
  - `me.mbe.algorithms.nextnodepred.Alg008`: Store also on some neighboring nodes: Not mentioned in the original thesis
  - `me.mbe.algorithms.nextnodepred.Alg012`: Fusion Multi Order Markov Model.
- Startup Prediction
  - `me.mbe.algorithms.startuppred.Alg011`: Store for short pauses.
  - `me.mbe.algorithms.startuppred.Alg013`: Do not store anything after shutdown: Used together with the algorithms for next node prediction: Not mentioned explicitly in the original thesis
  - `me.mbe.algorithms.startuppred.Alg014`: Store if short pause predicted.
  - `me.mbe.algorithms.startuppred.Alg015`: Clustering of startup times for long pauses: Not mentioned explicitly in the original thesis, just as a side note.

[1]: https://www.microsoft.com/en-us/download/details.aspx?id=52367
