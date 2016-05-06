#! /bin/sh

MODULES_FILE=nmi/build-modules.config

echo
echo Which java are we running
which java
java -version

echo
echo What about javac
which javac

echo
echo Where is ant
which ant

echo Starting Kepler build...
cd kepler/modules/build-area

echo Reading modules to be built...
for module in `cat $MODULES_FILE`
do
    echo "***************************************************" 
    echo "Building $module..." 
    echo "***************************************************" 
    echo Issuing change-to for module $module...
    ant change-to -Dsuite=$module
    echo Attempting to compile module $module...
    ant clean-all compile
    echo Compile complete.
    echo Starting unit tests for module $module...
    ant test
    echo Unit tests complete.
    echo "Done with module $module."
    echo " "
done


# Package up installer as a results file that NMI will transfer to submit host
#echo -n "Creating results file in: "
#echo `pwd`
#tar czf results.tar.gz kepler-1.0.0.jar
#mv results.tar.gz ..
#cd ..
#echo -n "Moved results file to: "
#echo `pwd`
