import ij.gui.PolygonRoi
import qupath.lib.objects.PathObjects
import qupath.lib.analysis.features.ObjectMeasurements
import qupath.lib.objects.PathCellObject
import qupath.lib.objects.PathObjectTools
import qupath.lib.analysis.features.ObjectMeasurements
import ij.measure.Calibration
import ij.plugin.filter.EDM
import qupath.lib.measurements.MeasurementList;
import ij.process.ColorProcessor
import ij.process.FloatPolygon
import ij.process.FloatProcessor
import ij.process.ImageProcessor
import ij.process.ShortProcessor


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.filter.EDM;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import qupath.imagej.processing.MorphologicalReconstruction;
import qupath.imagej.processing.RoiLabeling;
import qupath.imagej.processing.SimpleThresholding;
import qupath.imagej.processing.Watershed;
import qupath.imagej.tools.IJTools;
import qupath.imagej.tools.PixelImageIJ;
import qupath.lib.analysis.images.SimpleImage;
import qupath.lib.analysis.stats.RunningStatistics;
import qupath.lib.analysis.stats.StatisticsHelper;
import qupath.lib.color.ColorDeconvolutionStains;
import qupath.lib.color.StainVector;
import qupath.lib.common.GeneralTools;
import qupath.lib.images.ImageData;
import qupath.lib.images.PathImage;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.images.servers.ServerTools;
import qupath.lib.measurements.MeasurementListFactory;
import qupath.lib.measurements.MeasurementList;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjectTools;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClassFactory;
import qupath.lib.plugins.AbstractTileableDetectionPlugin;
import qupath.lib.plugins.ObjectDetector;
import qupath.lib.plugins.parameters.DoubleParameter;
import qupath.lib.plugins.parameters.Parameter;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.regions.ImagePlane;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.PolygonROI;
import qupath.lib.roi.ShapeSimplifier;
import qupath.lib.roi.interfaces.ROI;

import qupath.imagej.processing.RoiLabeling
import qupath.imagej.processing.Watershed
import qupath.imagej.tools.IJTools
import qupath.imagej.tools.PixelImageIJ
import qupath.imagej.detect.cells.ObjectMeasurements
import qupath.lib.analysis.images.SimpleImage
import qupath.lib.analysis.stats.RunningStatistics
import qupath.lib.analysis.stats.StatisticsHelper
import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.color.StainVector
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.PathImage
import qupath.lib.images.servers.PixelCalibration
import qupath.lib.images.servers.ServerTools
import qupath.lib.objects.PathObjectTools
import qupath.lib.regions.ImagePlane
import qupath.lib.regions.RegionRequest
import qupath.lib.roi.ShapeSimplifier

import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import ij.process.ByteProcessor;
import ij.gui.Roi;
import qupath.lib.roi.PolygonROI;

def annot = getDetectionObjects()

//var annot = getSelectedObjects()
//	if ( annot.isEmpty() ) {
//   		createSelectAllObject(true)
//    	} 

double cellExpansionMicrons=3
//makeMeasurements=true //only tested with true
smoothBoundaries=true //only tested with true


//need a region slightly larger than actual annotation because the cell boundaries can be outside. There's probably a better way to do this.
int delta = 0 //must be even or ImageJ gets sad. In pixels.

def imageData = getCurrentImageData()
PixelCalibration Qcal=imageData.getServer().getPixelCalibration()
def pixelSize=Qcal.getAveragedPixelSize()

double cellExpansion = cellExpansionMicrons/pixelSize

ImageServer<BufferedImage> server = imageData.getServer()
double downsample = ServerTools.getDownsampleFactor(server, pixelSize);
double downsampleSqrt = Math.sqrt(downsample);
ColorDeconvolutionStains stains = imageData.getColorDeconvolutionStains();

//loop over annotations
//def annot=getAnnotationObjects()  // YOU MUST SELECT WHOLE SLIDE AS AN ANNOTATION
annot.each { a ->
    annotROI = a.getROI()

//Need ALL the coordinates
    int width = (int) (annotROI.getBoundsWidth() + delta)
    int height = (int) (annotROI.getBoundsHeight() + delta)
    int left = (int) (annotROI.getBoundsX().toInteger() - delta / 2)
    int top = (int) (annotROI.getBoundsY().toInteger() - delta / 2)
    ImagePlane plane = ImagePlane.getPlane(annotROI)
    
//black image to put nuclei on top of
    ByteProcessor bp = new ByteProcessor(width, height)
    bp.setValue(255)

//Actual image of the islet
    PathImage pathImage = IJTools.convertToImagePlus(server, RegionRequest.createInstance(server.getPath(), downsample, left, top, width, height));
    ip = pathImage.getImage().getProcessor()
    Calibration cal = pathImage.getImage().getCalibration();

//turn nuclei objects into a black and white image
    def nucleiObjs=a.getChildObjects().findAll{it.isDetection()}
    def nuclei = nucleiObjs.collect { it.getROI() }
    List<PolygonRoi> roisNuclei = new ArrayList<>()
    nuclei.each {
        Roi roiIJ = IJTools.convertToIJRoi(it, cal, downsample)
        roisNuclei.add(roiIJ)
        bp.fill(roiIJ);
    }

//bp is now a black and white image with nuclei.

//create label image of nuclei (1-# nuclei, 0 = background).
    ShortProcessor ipLabels = new ShortProcessor(width, height)
    RoiLabeling.labelROIs(ipLabels, roisNuclei);
//ipLabels is now a label image of the nuclei .

/**Now we are ready to find the cell boundaries**/
//Euclidean Distance Map of distance to nuclei.
    FloatProcessor fpEDM = new EDM().makeFloatEDM(bp, (byte) 255, false)
    fpEDM.multiply(-1);
//fpEDM = 0 at all nuclei pixels, increasingly negative as you get further away

//watershed segmentation on binary nuclei image, up to the distance given by cellExpansion
    ImageProcessor ipLabelsCells = ipLabels.duplicate()
    double cellExpansionThreshold = -cellExpansion;
    Watershed.doWatershed(fpEDM, ipLabelsCells, cellExpansionThreshold, false);
//ipLabelCells now is a label image, like ipLabels, but larger for the cells. Already watershed segmented.

//turn label image into rois (imageJ rois, not yet qupath rois)
    PolygonRoi[] roisCells = RoiLabeling.labelsToFilledROIs(ipLabelsCells, roisNuclei.size());

// Create labelled image for cytoplasm, i.e. remove all nucleus pixels
    for (int i = 0; i < ipLabels.getWidth() * ipLabels.getHeight(); i++) {
        if (ipLabels.getf(i) != 0)
            ipLabelsCells.setf(i, 0f);
    }

//turns the ImageJ rois into QuPath ROIs
    List<PathObject> pathObjects = new ArrayList<>() //initialize
    for (int i = 0; i < roisCells.length; i++) {
        //actually add the measurements to the objects
        PolygonRoi rN = roisNuclei.get(i)
        PolygonRoi r = roisCells[i]
        def nucleusObj = nucleiObjs[i]

        if (r == null)
            continue;

        //smooth boundaries of cell polygons
        if (smoothBoundaries) {
            r = new PolygonRoi(r.getInterpolatedPolygon(1, false), Roi.POLYGON);
            r = smoothPolygonRoi(r);
            r = new PolygonRoi(r.getInterpolatedPolygon(Math.min(2, r.getNCoordinates().toDouble() * 0.1), false), Roi.POLYGON);
        }
        PolygonROI pathROI = IJTools.convertToPolygonROI(r, cal, downsample, plane);
        if (smoothBoundaries)
            pathROI = ShapeSimplifier.simplifyPolygon(pathROI, downsampleSqrt / 2.0);
        // Create & store the cell object
        PathObject pathObject = PathObjects.createCellObject(pathROI, nucleusObj == null ? null : nucleusObj.getROI(), null, measurementList);
        pathObjects.add(pathObject);
    }

    for (PathObject pathObject : pathObjects)
        pathObject.getMeasurementList().close();

// Sometimes smoothing can cause nuclei of cell boundaries to be removed - in this case,
// filter out the invalid ROIs now
    int sizeBefore = pathObjects.size();
    pathObjects.removeIf(p -> PathObjectTools.getROI(p, false).isEmpty() ||
            PathObjectTools.getROI(p, true).isEmpty());
    int sizeAfter = pathObjects.size();
    if (sizeBefore != sizeAfter) {
        logger.debug("Filtered out {} invalid cells (empty ROIs)", sizeBefore - sizeAfter);
    }

//add the objects to the hierarchy and remove the (now duplicated) nuclei
    addObjects(pathObjects)
    removeObjects(nucleiObjs, true)
}
resolveHierarchy()
print('Cell Expansion Complete')




private static PolygonRoi smoothPolygonRoi(PolygonRoi r) {
    FloatPolygon poly = r.getFloatPolygon();
    FloatPolygon poly2 = new FloatPolygon();
    int nPoints = poly.npoints;
    for (int i = 0; i < nPoints; i += 2) {
        int iMinus = (i + nPoints - 1) % nPoints;
        int iPlus = (i + 1) % nPoints;
        poly2.addPoint((poly.xpoints[iMinus] + poly.xpoints[iPlus] + poly.xpoints[i])/3,
                (poly.ypoints[iMinus] + poly.ypoints[iPlus] + poly.ypoints[i])/3);
    }
//			return new PolygonRoi(poly2, r.getType());
    return new PolygonRoi(poly2, Roi.POLYGON);
}






















