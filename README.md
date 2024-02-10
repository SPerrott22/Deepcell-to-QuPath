# Importing Masks from DeepCell to QuPath

[![jupyter](https://img.shields.io/badge/Jupyter-Lab-F37626.svg?style=flat&logo=Jupyter)](https://jupyterlab.readthedocs.io/en/stable)
[![python](https://img.shields.io/badge/Python-3.8-blue.svg?style=flat&logo=python&logoColor=white)](https://www.python.org)
[![tensorflow](https://img.shields.io/badge/TensorFlow-2.8.4-FF6F00.svg?style=flat&logo=tensorflow)](https://www.tensorflow.org)
[![qupath](https://img.shields.io/badge/QuPath-4.0-blue)](https://qupath.github.io/)
[![paquo](https://img.shields.io/badge/PAQUO-0.7.1-purple)](https://paquo.readthedocs.io/)
[![deepcell-tf](https://img.shields.io/badge/DeepCell-0.12.9-green)](https://deepcell.readthedocs.io/)

Welcome to an open-source DeepCell to QuPath tutorial! This document contains several scripts that when run together allow you to apply DeepCell's Mesmer model for detecting cell nuclei in QuPath. Note: you can already do this for small images via ImageJ's DeepCell plugin, but this pipeline is designed for processing whole-slide images where you have tens of thousands or hundreds of thousands of cells, that is, images that are too large to pass into ImageJ.

Shoutout to the [Image.SC Forum](https://forum.image.sc/), especially Pete Bankhead, for helping me with figuring this out!

## Description

This pipeline connects a deep-learning powered cell segmentation model to open-source software that can perform neighborhood analysis and clustering.

Using this pipeline will convert Mesmer-generated instance segmentation masks of nuclei from a fluorescence ome-tiff to PathCellObjects in QuPath with auto-generated cytoplasms, from which a csv containing x/y coordinates and marker intensities per cell can be readily downloaded via QuPath's user interface. In other words, it is like using QuPath's StarDist plugin but with a deep learning model.

## How To Use

Prerequisites: desktop version of QuPath installed

Time Estimate for standard CODEX-output with ~20k cells: 1 hour 22 minutes
- Step 0: 2 minutes
- Step 1: 17 minutes
- Step 2: 2 minutes
- Step 3: 60 minutes
- Step 4: 1 minute


### Step 0: Image Pre-Processing

Note: if your fluorescence image is already in the right format for QuPath to read and understand, you may skip this step.

1. Obtain your multiplex fluorescence tiff from CODEX and save as a .tif file.

2. The CODEX outputted tiff doesn't have the required metadata for QuPath to recognize the channels. Therefore, do the following:

   - Open the CODEX tiff in QuPath. It should be monochrome as of now. Then open [step-0.groovy](step-0.groovy) using Scripts --> Open Script Editor --> File --> Open.

   - Run the groovy script and save the ome tiff.

### Step 1: Generating Nuclei Masks via Mesmer

DeepCell's Mesmer deep learning model is specifically designed to segment cells in multiplex fluorescence images. It can either simply segment the nuclei based on a DAPI channel or it can segment both nuclei and cytoplasms if there is a cytoplasmic channel.

The default configuration of this pipeline assumes there is no cytoplasmic channel in your image.

The following step will require at least 75 GB of RAM. You may choose to run it in Google Colab Pro (in which case use [step-1-colab.ipynb](step-1-colab.ipynb)) or on a suitable server or desktop.

1. Open [step-1.ipynb](step-1.ipynb) and modify the file path to be that of your ome tiff.

2. Run the notebook. It should save a new tiff image `ome_mask_2D.tif` that is an instance segmentation mask of your image's DAPI.

Note: there are optional cells containing code for visualization of the masks. You can uncomment lines and adjust image slices to display channels you want to visualize as well as the mask overlay.

# Step 2: Pseudo-Cytoplasm Generation and Transferral to QuPath

Note: In the future, if `paquo` API implements an `add_cellObject()` method, this current setup may be updated to require fewer scripts. Regardless, the 1-hour bottleneck is caused by measurements for every cell in QuPath.

Comparison of both methods:


---
## Method 1: Voronoi Tessellation in Groovy

### Converting Masks Into QuPath PathDetectionObjects

1. Open [step-2.ipynb](step-2.ipynb) on your desktop (or wherever you have QuPath installed).

2. Make sure you have necessary Python dependencies installed via `pip install -r requirements.txt`

3. Ensure `QuPath.exe` resides in usr/bin so that `paquo` can readily find it

4. Run the cells (3 minutes or so for ~20,000 DAPI). This automatically creates a new QuPath project in the specified folder and adds the DAPI masks as detection objects to your fluorescence image.

### Convert the PathDetectionObjects into PathCellObjects

Note: if you do not care about creating pseudo-cytoplasms for your cells, you may skip this step or modify [step-3.groovy](step-3.groovy) simply to perform measurements.

This is by far the most time-consuming step as it can take an hour or so for QuPath to do measurments per cell on an image with ~20,000 DAPI.

The point of creating the pseudo-cytoplasms is to have a rough estimate of what the cytoplasms should be given that we have no cytoplasmic marker. This can help us with cell classification downstream as it is easier for markers to be assigned to cells.

1. Open the QuPath project you created in the previous step

2. Open the [step-3.groovy](step-3.groovy) via Scripts --> Open Script Editor --> File --> Open and run.

3. Wait for about an hour as it uses Voronoi tessellation to generate a cytoplasm for each nuclei and make measurements of shape and marker intensities within the ROI regions.

---

## Method 2: Cell Expansion in Python

If you would rather do cell expansion in Python instead of relying on Groovy's Voronoi tessellation, proceed with the following:

1. On a server with high compute resources, generate the cytoplasms with step-3a.ipynb and save the numpy array instance labels for both your DAPI and Cytoplasms to your local computer where QuPath is installed.

2. Run masks-to-QuPath.ipynb with the correct file path names to add both the cytoplasms and nuclei as detection objects to a new QuPath project.

3. Run split-cells.groovy to convert these detections into cellObjects and generate measurements.

---

### Step 4: Proceed to Do Cell Classification, Neighborhood Analysis, Visualization, etc.

Now your CellObjects should look just as if they were created via StarDist but they used DeepCell instead! You can proceed to train a cell classification model in QuPath or you can save the .csv representing your cells and their measurements and read in the data to Python/R/MATLAB to perform your own neighborhood analysis.

To export measurements from QuPath:

- Go to Measurements --> Export as CSV

Example of neighborhood analysis to be added.

## Credits

This pipeline was put together as part of my research under [Professor Willy Hugo](https://www.uclahealth.org/cancer/members/willy-hugo) at UCLA Health.

Sincere appreciation for the support of the Image.SC Forum, especially Pete Bankhead for helping write some of these scripts and answering key questions about how to work with QuPath's scripting interface!

[![UCLA](https://logos-world.net/wp-content/uploads/2021/11/UCLA-Logo.png)](https://www.ucla.edu)
