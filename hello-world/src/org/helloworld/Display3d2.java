package org.helloworld;

import ij.IJ;
import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.FilePortParameter;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import java.io.File;

public class Display3d2 extends LimitedFiringSource {

    public PortParameter directory;

    public Parameter listOnlyDirectories;

    public FilePortParameter directoryOrURL;

    /** If true, and <i>directoryOrURL</i> refers to a local directory (not a URL),
     *  then only files will be listed on the output. If <i>directoryOrURL</i>
     *  is a URL, then this parameter is ignored (there appears to be no reliable
     *  way to tell whether the URL refers to a directory or file).
     *  This is a boolean that defaults to false.
     */
    public Parameter listOnlyFiles;

    /** If true, and <i>directoryOrURL</i> refers to a local directory
     *  (not a URL), that is empty, then the output will be empty
     *  string array and no exception is reported. If
     *  <i>directoryOrURL</i> is a URL, then this parameter is ignored
     *  (there appears to be no reliable way to tell whether the URL
     *  refers to a directory or file).  This is a boolean that
     *  defaults to false.
     */
    public Parameter allowEmptyDirectory;

    public Display3d2(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);
        output.setTypeEquals(BaseType.STRING);
        directory = new PortParameter(this, "directory");
        directory.setStringMode(true);

        // Tell the file browser to allow only selection of directories.
        directoryOrURL = new FilePortParameter(this, "FILEorURL");
        new Parameter(directoryOrURL, "allowFiles", BooleanToken.TRUE);
        new Parameter(directoryOrURL, "allowDirectories", BooleanToken.TRUE);

        listOnlyDirectories = new Parameter(this, "displayAsVolume");
        listOnlyDirectories.setTypeEquals(BaseType.BOOLEAN);
        listOnlyDirectories.setExpression("false");

        listOnlyFiles = new Parameter(this, "listOnlyFiles");
        listOnlyFiles.setTypeEquals(BaseType.BOOLEAN);
        listOnlyFiles.setExpression("false");

        allowEmptyDirectory = new Parameter(this, "displayAsOrtho");
        allowEmptyDirectory.setTypeEquals(BaseType.BOOLEAN);
        allowEmptyDirectory.setExpression("false");

    }

    @Override
    public void fire() throws IllegalActionException {
        // TODO Auto-generated method stub
        super.fire();
        directory.update();

        // Get all information.
        directory.update();

        String _path;
        File _dir;
        boolean _mkdirsSuccess;

        _path = ((StringToken) directory.getToken()).stringValue();



//Display 200 sequence that forms entire head
        //String path = new String("/home/elblonko/Desktop/ExportedDICOM/0.9-200-Sequencetiff");

        //Display a 20 segment slice of the middle section of the brain
        //String path = new String("/home/elblonko/Desktop/ExportedDICOM/WholeHeadtiff");


        //ImagePlus dicomStackImage = IJ.openImage(_path);

        ImagePlus dicomStackImage = (ImagePlus)IJ.runPlugIn("Nifti_Reader", _path);

        new StackConverter(dicomStackImage).convertToGray8();

        //need to add this library
        Image3DUniverse univ = new Image3DUniverse();
        univ.show();

        // Add the image as a volume rendering
        /*
        In this case, the stack is displayed as a volume rendering. Volume renderings
        are created here by putting 2D slices of the stack one behind another. Different
        planes are thereby separated according to the pixel dimensions of the image. To
        each voxel, a transparency value is assigned, depending on its intensity. Black
        voxels are thereby fully transparent, whereas white ones are fully opaque.
         */
        Content c;
        //c = univ.addVoltex(dicomStackImage);

        c = univ.addOrthoslice(dicomStackImage);


        // Display the image as orthslices
        /*
        There exist 4 types of Contents: Volume Renderings, Orthoslices, Isosurfaces
        and Surface Plots. Contents which were created from an ImagePlus can be
        transferred from one display type into another:
         */
        //c.displayAs(Content.MULTIORTHO);

        // Remove the Content from the universe
        //univ.removeContent(c.getName());

        /*
        Isosurfaces are surfaces which are generated here by applying the marching
        cubes algorithm. This algorithm assumes that there is one intensity value
        in the stack which separates a 3D object from its background. Therefore,
        the generated surface has theoretically everywhere the same (iso-)value.
        This value, also called the threshold of the surface, is adjustable, as will
        be seen later.
         */
        // Add an isosurface
        //c = univ.addMesh(dicomStackImage);


         /*
         Finally, a Content can be displayed as a surface plot. A surface plot is
         a 3D representation of a 2D slide, where the 3rd dimension is formed by
         the image intensity
          */
        //c = univ.addSurfacePlot(dicomStackImage);

        // remove all contents
        //univ.removeAllContents();
        // close
        //univ.close();

        /*
        //Create a movie
        // animate the universe
        univ.startAnimation();


        // record a 360 degree rotation around the y-axis
        ImagePlus movie = univ.record360();
        movie.show();
        univ.pauseAnimation();
        */



    }






        //String usernameStr = ( (StringToken)username.getToken() ).stringValue();
        //output.send(0, new StringToken("Hello " + usernameStr + "!"));
    }

