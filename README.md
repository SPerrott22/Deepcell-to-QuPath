# Deepcell-to-QuPath
[![jupyter](https://img.shields.io/badge/Jupyter-Lab-F37626.svg?style=flat&logo=Jupyter)](https://jupyterlab.readthedocs.io/en/stable)
[![python](https://img.shields.io/badge/Python-3.8-blue.svg?style=flat&logo=python&logoColor=white)](https://www.python.org)
[![tensorflow](https://img.shields.io/badge/TensorFlow-2.8.4-FF6F00.svg?style=flat&logo=tensorflow)](https://www.tensorflow.org)
[![qupath](https://img.shields.io/badge/QuPath-v4.0)](https://qupath.github.io/)

This Pipeline connects AI-powered Cell Segmentation to open-source software to perform neighborhood analysis and clustering. Specifically, it converts Deepcell generated instance segmentation masks of nuclei from an ome-tiff to PathCellObjects in QuPath, from which a csv containing x/y coordinates and marker intensities per cell can be derived. Compare this to using QuPath's StarDist plugin.

Prerequisites: desktop version of QuPath installed

### Step 1: Image Pre-Processing

Convert CODEX-outputted multichannel tiff into one that is readable into QuPath. The CODEX outputted tiff doesn't have the required metadata for QuPath to recognize the channels. Therefore, do the following:

Open the CODEX tiff in QuPath. It should be monochrome as of now. Then open the ometiff groovy using Scripts --> Open Script Editor --> File --> Open. Run the groovy script and save the ome tiff.

### Step 2: Generating AI Masks

Using Google Colab with at least 75 GB CPU or a similar device, run the .ipynb with the ome tiff file. Save the mask.

### Step 3: Converting Masks Into QuPath PathDetectionObjects

Open the paquo .ipynb on your desktop and run with the mask and tiff files. It will open up a QuPath project with the masks read as ROIs.

### Step 4: Run the Convert-to-Cells.groovy

Open the .groovy in the Script Editor in QuPath via Scripts --> Open Script Editor --> File --> Open. Run the script. It will watershed expand around the nuclei and make measurements of marker intensities within the ROI regions.

### Step 5: Save the .csv and open in Python or R to perform neighborhood analysis

Go to Measurements --> Export as CSV. Save this file and read in its contents into Python or R to perform clustering, neighborhood analysis, etc. visualizations! (Example to be added)
