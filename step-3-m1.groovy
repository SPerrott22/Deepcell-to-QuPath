/**
 * Create cells with Voronoi-generated cytoplasms from nuclei detections.
 * This usually takes an hour or so for large images e.g. 20,000 cells.
 * The long time is mainly caused by generating measurements.
 */

import qupath.lib.objects.PathObjects
import qupath.lib.analysis.features.ObjectMeasurements
import qupath.lib.objects.PathCellObject
import qupath.lib.objects.PathObjectTools
import qupath.lib.objects.CellTools

var imageData = getCurrentImageData()
var server = getCurrentServer()
var pathObjects = getSelectedObjects()
	if ( pathObjects.isEmpty() ) {
   		createFullImageAnnotation(true)
    	}    	
var cal = server.getPixelCalibration()
var downsample = 1.0

double pixelSize = getCurrentImageData().getServer().getPixelCalibration().getAveragedPixelSize()

println("Extracting ROIs...")
def detections = getDetectionObjects() // .findAll {d -> !d.isCell()}

println("Creating cells with cytoplasms generated via Voronoi...") // takes about 20 minutes
def cells = CellTools.detectionsToCells(detections, 10, -1)
/*
`detections` - the detection objects from which to create the cells; these define the nuclei
`distance` - the maximum distance (in pixels) to expand each nucleus
`nucleusScale` - the maximum size of the cell relative to the nucleus (ignored if ≤ 1).
*/

println("Removing detections...")
removeObjects(detections, true)
println("Adding cells...")
addObjects(cells)

// add measurements below

println("Adding measurements...")
ObjectMeasurements.addShapeMeasurements(cells, cal)

def measurements = ObjectMeasurements.Measurements.values() as List
def compartments = ObjectMeasurements.Compartments.values() as List
cells.parallelStream().forEach { cell ->
    ObjectMeasurements.addIntensityMeasurements(server, cell, downsample, measurements, compartments)
}

fireHierarchyUpdate()
println("Done!")
