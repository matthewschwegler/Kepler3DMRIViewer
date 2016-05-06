package org.display3d;

import ij.IJ;
import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.Image3DUniverse;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.parameters.FilePortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;



/**
 * Created by elblonko on 7/29/14.
 */
public class Display3d extends TypedAtomicActor {


    /*Used to take in a path to a file or input the path
    manually.
     */
    //public PortParameter directory;

    /*
    Used to have a file browser GUI in the actor window
    user can pass in a file or can use the browse feature
    of the actor itself
     */
    public FilePortParameter directory;

    /*
    Check this parameter if the file you are choosing to display
    is of nifti file type. Allows ImageJ to open such a file.
     */
    public Parameter niftiFileType;

    public Parameter displayAsOrtho;

    public Parameter displayAsVolume;

    //Constructor
    public Display3d(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

        /*
        directory = new PortParameter(this, "directory");
        directory.setStringMode(true);
        */



        // Tell the file browser to allow only selection of directories.
        directory = new FilePortParameter(this, "fileOrURL");
        new Parameter(directory, "allowFiles", BooleanToken.TRUE);
        new Parameter(directory, "allowDirectories", BooleanToken.TRUE);

        niftiFileType = new Parameter(this, "niftiFileType");
        niftiFileType.setTypeEquals(BaseType.BOOLEAN);
        niftiFileType.setExpression("false");

        displayAsVolume = new Parameter(this, "displayAsVolume");
        displayAsVolume.setTypeEquals(BaseType.BOOLEAN);
        displayAsVolume.setExpression("false");

        displayAsOrtho = new Parameter(this, "displayAsOrtho");
        displayAsOrtho.setTypeEquals(BaseType.BOOLEAN);
        displayAsOrtho.setExpression("false");

    }

    public void fire() throws IllegalActionException {
        super.fire();

        // Get all information.
        directory.update();

        /*
        Used for PortParameter string arguments as a directory
         */
        _path = ((StringToken) directory.getToken()).stringValue();

        /*
        Takes any passed in parameter from the user
         */
        //URL _path = directory.asURL();


        _niftiFileType = ((BooleanToken) niftiFileType.getToken())
                .booleanValue();

        if(_niftiFileType){
            _img = (ImagePlus)IJ.runPlugIn("Nifti_Reader", _path);
        }
        else{
            _img = IJ.openImage(_path);
        }

        //does grayscale converstion required by 3D_viewer
        new StackConverter(_img).convertToGray8();

        _displayAsVolume = ((BooleanToken) displayAsVolume.getToken())
                .booleanValue();
        if(_displayAsVolume) {
            _univVolume = new Image3DUniverse();
            _univVolume.show();

            _v = _univVolume.addVoltex(_img);
        }

        _displayAsOrtho = ((BooleanToken) displayAsOrtho.getToken())
                .booleanValue();
        if(_displayAsOrtho){
            _univOrtho = new Image3DUniverse();
            _univOrtho.show();

            _o = _univOrtho.addOrthoslice(_img);
        }

        // Add the image as a volume rendering
        /*
        In this case, the stack is displayed as a volume rendering. Volume renderings
        are created here by putting 2D slices of the stack one behind another. Different
        planes are thereby separated according to the pixel dimensions of the image. To
        each voxel, a transparency value is assigned, depending on its intensity. Black
        voxels are thereby fully transparent, whereas white ones are fully opaque.
         */
        //Content c;
        //c = _univVolume.addVoltex(_img);

        //c = _univVolume.addOrthoslice(_img);


        // Display the image as orthslices
        /*
        There exist 4 types of Contents: Volume Renderings, Orthoslices, Isosurfaces
        and Surface Plots. Contents which were created from an ImagePlus can be
        transferred from one display type into another:
         */
        //c.displayAs(Content.MULTIORTHO);

        // Remove the Content from the universe
        //_univVolume.removeContent(c.getName());

        /*
        Isosurfaces are surfaces which are generated here by applying the marching
        cubes algorithm. This algorithm assumes that there is one intensity value
        in the stack which separates a 3D object from its background. Therefore,
        the generated surface has theoretically everywhere the same (iso-)value.
        This value, also called the threshold of the surface, is adjustable, as will
        be seen later.
         */
        // Add an isosurface
        //c = _univVolume.addMesh(_img);


         /*
         Finally, a Content can be displayed as a surface plot. A surface plot is
         a 3D representation of a 2D slide, where the 3rd dimension is formed by
         the image intensity
          */
        //c = _univVolume.addSurfacePlot(_img);

        // remove all contents
        //_univVolume.removeAllContents();
        // close
        //_univVolume.close();

        /*
        //Create a movie
        // animate the universe
        _univVolume.startAnimation();


        // record a 360 degree rotation around the y-axis
        ImagePlus movie = _univVolume.record360();
        movie.show();
        _univVolume.pauseAnimation();
        */



    }


    private String _path;

    private boolean _niftiFileType;

    private boolean _displayAsVolume;

    private boolean _displayAsOrtho;

    private ImagePlus _img;

    private Image3DUniverse _univVolume;

    private Image3DUniverse _univOrtho;

    private Content _v;

    private Content _o;

}
