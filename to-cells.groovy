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
def measurements = ObjectMeasurements.Measurements.values() as List
def compartments = ObjectMeasurements.Compartments.values() as List

for (detection in detections) {
    ObjectMeasurements.addIntensityMeasurements(server, detection, downsample, measurements, compartments)
}

def cells = detections.collect {
    return PathObjects.createCellObject(it.getROI(), it.getROI(), it.getPathClass(), it.getMeasurementList())    
}

//
//
//def annotations = detections.collect {
//    return PathObjects.createAnnotationObject(it.getROI(), it.getPathClass())
//}

removeObjects(detections, true)
addObjects(cells)
//
//for (annotation in annotations) {
//    def pathClass = getPathClass("DAPI")
//    annotation.setPathClass(pathClass)
//}
fireHierarchyUpdate()



//resolveHierarchy()

//def newDetections = new ArrayList<>()
//for (cellAnnotation in annotations) {
//    def nucleus = PathObjectTools.getDescendantObjects(cellAnnotation, null, null)
//    if (nucleus[0] == null) {continue;}
//    else if (nucleus.size() == 1) {
//        def roiNuc = nucleus[0].getROI()
//        def roiCyto = cellAnnotation.getROI()
//        def nucMeasure = nucleus[0].getMeasurementList()
//        def cell = new PathCellObject(roiCyto, roiNuc, cellAnnotation.getPathClass(), nucMeasure)
//        newDetections.add(cell)
//        print("Adding " + cell)
//    }
//    else {
//        print(nucleus.size())
//        def roiCyto = cellAnnotation.getROI()
//        def Acell = new PathCellObject(roiCyto, null, getPathClass('toAnnotation'), null)
//        newDetections.add(Acell)
//        print("Adding without nucleus " + Acell)
//    }
//}

//newDetections.each{addObject(it)}



//println( "Adding cell objects. Please wait..." )
//cells = []
//    nuclei.each{ nucleus ->      
//
//            cells.add( PathObjects.createCellObject( nucleus.getROI(), nucleus.getROI(), getPathClass( "Tissue" ), null ) )
//            }
//            
//            // expand the first roi watershed
        
//addObjects( cells )