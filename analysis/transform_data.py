import pickle
from datetime import datetime
import os

INPUT_DIR = "geolife_data\Data"
OUTPUT_DIR = "./geolife_data_transformed"

# Make transformed data directory
if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

# Transform the data
data_list = []
print("Starting to transform the data...")
for user_name in os.listdir(INPUT_DIR):
    # Get all users
    user_path = os.path.join(INPUT_DIR, user_name, "Trajectory")
    for subdir, dirs, files in os.walk(user_path):
        # Get all files for a single user
        filter(lambda f: f.endswith(".plt"), files)
        files.sort()
        locations = []
        for file_name in files:
            file_path = os.path.join(user_path, file_name)
            # Read the file
            with open(file_path, "r") as f:
                # Skip first 6 lines
                for line in range(6):
                    next(f)
                for line in f:
                    line = line.strip()
                    values = line.split(",")
                    STLoc = {"lat": values[0], "long": values[1], "time": datetime.strptime(values[5] + " " + values[6], '%Y-%m-%d %H:%M:%S')}
                    locations.append(STLoc)
        user_data = {"userID": user_name, "locations": locations}
        output_file = os.path.join(OUTPUT_DIR, user_name) + ".pkl"
        with open(output_file, "wb") as out:
            pickle.dump(user_data, out)
        print(f"User {user_name} finished.")
        