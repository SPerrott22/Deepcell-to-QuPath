// Time to Run for Whole CODEX Image: 2:26:32

import qupath.lib.gui.dialogs.Dialogs
def pathModel = "stardist_cell_seg_model.pb"
import qupath.ext.stardist.StarDist2D

def stardist = StarDist2D.builder(pathModel)
        .threshold(0.5)              // Probability (detection) threshold
        .channels(0)            // Select detection channel
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(0.5)              // Resolution for detection
        .cellExpansion(5.0)          // Approximate cells based upon nucleus expansion
        // .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
        .includeProbability(true)    // Add probability as a measurement (enables later filtering)
        .build()

// Run detection for the selected objects
def imageData = getCurrentImageData()

var pathObjects = getSelectedObjects()
    if ( pathObjects.isEmpty() ) {
           createFullImageAnnotation(true)
        }        

stardist.detectObjects(imageData, pathObjects)
println 'Done!'