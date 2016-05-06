kepler
======
Description:
This repository contains the entire Kepler program with my integrated 3d-viewer actor. 
You can download the entire kepler environment I developed the actor in here or you can simply download 
all the contents of the 3d-viewer folder (top of the list) and plug that directly into your
/home/kepler directory and compile.

Instantiate the Actor:
Open a kepler workflow and select the tools->instantiate component. 
In the pop up window time in org.display3d.Display3d. The actor will appear in your workflow.
Actor Options:
The actor takes a path to the file to be displayed as a parameter. Additionally inside the actor you have 3 options.
Options 1: NiftiFileType: Check this box if the input file is of Nifti format otherwise leave unchecked.
Option 2: DisplayAsVolume: Check if you would like to display a 3d volume of the input.
Option 3: DisplayAsOrtho: Check if you would like to display as a series of slices in 3d.

Description of Actor:
The 3d-viewer actor works by implementing several API’s based on the NIH’s ImageJ software. 
It can take a variety of input types such as DICOM, NIFTI or .TIFF files.

