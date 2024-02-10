// 1:14:05
import qupath.lib.objects.PathObjects
import qupath.lib.analysis.features.ObjectMeasurements
import qupath.lib.objects.PathCellObject
import qupath.lib.objects.PathObjectTools
import qupath.lib.analysis.features.ObjectMeasurements

var imageData = getCurrentImageData()
var server = getCurrentServer()
var pathObjects = getSelectedObjects()
	if ( pathObjects.isEmpty() ) {
   		createSelectAllObject(true)
    	}    	
var cal = server.getPixelCalibration()
var downsample = 1.0

double pixelSize = getCurrentImageData().getServer().getPixelCalibration().getAveragedPixelSize()

def detections = getDetectionObjects()

// Assuming detection names are properly ordered and start from 0
// Adjust the size of lists based on the expected number of cytoplasms and nuclei
int expectedSize = detections.size() / 2 // Assuming equal number of cytoplasms and nuclei

def cytoplasms = [] * expectedSize
def nuclei = [] * expectedSize

println("Extracting ROIs...")
detections.each { detection ->
    def nameParts = detection.getName().split(/\s+/)
    def type = nameParts[0]
    def index = nameParts[1].toInteger()
    if (type == "Nucleus") {
        nuclei[index] = detection
    } else if (type == "Cytoplasm") {
        cytoplasms[index] = detection
    }
}

def cells = []

println("Creating cells...")
for (int i = 0; i < nuclei.size(); i++) {
    cells << PathObjects.createCellObject(cytoplasms[i].getROI(), nuclei[i].getROI(), null, null)
}
//     if (cytoplasms[i] && nuclei[i]) {

println("Removing detections...")
removeObjects(detections, true)
println("Adding cells...")
addObjects(cells)

println("Adding measurements...")
def measurements = ObjectMeasurements.Measurements.values() as List
def compartments = ObjectMeasurements.Compartments.values() as List
def shape = ObjectMeasurements.ShapeFeatures.values() as List

for ( cell in cells ) {
    ObjectMeasurements.addIntensityMeasurements( server, cell, downsample, measurements, compartments )
    ObjectMeasurements.addCellShapeMeasurements( cell, cal,  shape )
}

fireHierarchyUpdate()
println("Done!")