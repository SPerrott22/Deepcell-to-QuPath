# Deepcell-to-QuPath
This Pipeline connects AI-powered Cell Segmentation to open-source software to perform neighborhood analysis and clustering. Specifically, it converts Deepcell generated instance segmentation masks of nuclei from an ome-tiff to PathCellObjects in QuPath, from which a csv containing x/y coordinates and marker intensities per cell can be derived. Compare this to using QuPath's StarDist plugin.

Prerequisites: desktop version of QuPath installed

### Step 1: Image Pre-Processing

Convert CODEX-outputted multichannel tiff into one that is readable into QuPath. The CODEX outputted tiff doesn't have the required metadata for QuPath to recognize the channels. Therefore, do the following:

Open the CODEX tiff in QuPath
