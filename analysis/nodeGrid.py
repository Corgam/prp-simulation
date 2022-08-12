from numpy import ceil, sqrt
import numpy as np

class Location():
    """
    A location object consiting of latitude and longitude coordinates
    """
    def __init__(self, latitude: float, longitude: float) -> None:
        self.latitude = latitude
        self.longitude = longitude
    def getLatitude(self):
        return self.latitude
    def getLongitude(self):
        return self.longitude

def getNodeGrid(numOfNodes: int):
    """
    Returns a grid of nodes from a given number of total nodes.
    """
    n = int(ceil(sqrt(numOfNodes)))
    return getNodeGrid2D(n,n)

def getNodeGrid2D(noNodesXaxis: int, noNodesYaxis: int):
    """
    Returns a grid of nodes from a given number of nodes in X and Y axes.
    """
    return NodeGrid(Location(40.2, 116.1), Location(39.71, 116.74), noNodesXaxis, noNodesYaxis)

centresList = []
class NodeGrid():
    """
    A NodeGrid object containing a list of node centres, as well as helper functions.
    """
    def __init__(self, nwCorner: Location, seCorner: Location, noNodesWE: int, noNodesNS: int) -> None:
        self.centres, self.WECentresLocs, self.NSCentresLocs = self.__calculateTheCentres(nwCorner, seCorner, noNodesWE, noNodesNS)
        assert len(self.centres) == noNodesWE * noNodesNS
        self.nwCorner = nwCorner
        self.seCorner = seCorner
        self.noNodesWE = noNodesWE
        self.noNodesNS = noNodesNS

    def __calculateTheCentres(self, nwCorner: Location, seCorner: Location, noNodesWE: int, noNodesNS: int):
        """
        Calculates the node centres' coordinates for the initialization.
        """
        # Calculate the WE centres' coordinates
        weDistance = nwCorner.longitude - seCorner.longitude
        weStepSize = weDistance / noNodesWE
        WECentresLocs = []
        for i in range(1, noNodesWE):
            WECentresLocs.append(nwCorner.longitude - i * weStepSize)
        # Calculate the SN centres' coordinates
        snDistance = seCorner.latitude - nwCorner.latitude
        snStepSize = snDistance / noNodesNS
        NSCentresLocs = []
        for i in range(1, noNodesNS):
            NSCentresLocs.append(seCorner.latitude - i * snStepSize)
        # Merge the centres' coordinates
        centres = dict()
        for i in range(noNodesNS):
            for j in range(noNodesWE):
                name = f"{i}-{j}"
                lat = seCorner.latitude - (i + 0.5)* snStepSize
                long = nwCorner.longitude - (j+ 0.5) * weStepSize
                centresList.append((lat,long))
                centres[name] = Location(lat, long)
        return centres, WECentresLocs, NSCentresLocs

    def closest_node(self, node, nodes):
        """
        Finds the closest values from a list of values
        Source: https://codereview.stackexchange.com/questions/28207/finding-the-closest-point-to-a-list-of-points
        """
        nodes = np.asarray(nodes)
        dist_2 = np.sum((nodes - node)**2, axis=1)
        return np.argmin(dist_2)

    def getClosestNodeCoords(self, loc: Location):
        """
        Find the coordinates of the closest centre node from the given location.
        """
        i = self.closest_node((float(loc.latitude), float(loc.longitude)), centresList)
        x,y = centresList[i]
        return Location(x,y)
    
    def getClosestNodeID(self, loc: Location):
        """
        Find the closest centre node's ID from the given location.
        """
        closest_node_location = self.getClosestNodeCoords(loc)
        # Look for a key from dict based on value
        node_id = [id for id,location in self.centres.items() if (location.latitude == closest_node_location.latitude and location.longitude == closest_node_location.longitude)]
        assert len(node_id) == 1
        return node_id[0]
    
    def getAllNodes(self):
        """
        Get a dictionary of all nodes from the grid
        """
        return self.centres