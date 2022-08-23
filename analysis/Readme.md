# Analysis files for temporal prp-simulation

## Structure

- `figures` folder contains all used figures
- Each plot has its own python file
- `helpers.py` file from the [original simulation](https://github.com/MalteBellmann/prp-simulation) 
- Other helper files

## Setup

1. Download the [geolife dataset](https://www.microsoft.com/en-us/download/details.aspx?id=52367&from=https%3A%2F%2Fresearch.microsoft.com%2Fen-us%2Fdownloads%2Fb16d359d-d164-469e-9fd4-daa38f2b2e13%2F) into the `geolife_data` folder.
2. Run the `transform_data.py` file.
3. Install the requirements from the `requirements.txt` file.
4. For each of the figures from the thesis, run a corresponsing python file in a debug mode. To get the plots, put a breakpoint at the last line.