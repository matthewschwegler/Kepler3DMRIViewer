#! /bin/sh
set -v on
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

## echo 
## echo shell vars
## set

## echo
## echo Env vars
## env
#export KEPLER=`pwd`/kepler
#export PTII=`pwd`/ptII

echo processing kepler...
cd kepler-run/modules/build-area

echo Issuing change-to...attempting to verify checked out modules
ant change-to -Dsuite=kepler

echo attempting to compile...
ant compile
echo compile complete...starting unit tests.

ant test
echo unit tests complete

echo creating kepler installer.
ant build-windows-installer -Dno-win-exe=true
ant build-mac-installer -Dno-mac-app=true


# Package up installer as a results file that NMI will transfer to submit host
#echo -n "Creating results file in: "
#echo `pwd`
#tar czf results.tar.gz kepler-1.0.0.jar
#mv results.tar.gz ..
#cd ..
#echo -n "Moved results file to: "
#echo `pwd`
