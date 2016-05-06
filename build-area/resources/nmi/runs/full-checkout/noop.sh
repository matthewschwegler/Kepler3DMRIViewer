#! /bin/sh

echo "Starting NOOP script"

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
export KEPLER=`pwd`/kepler
export PTII=`pwd`/ptII

echo "Creating fake jar output"
echo "This is a test jar file." > kepler/kepler-1.0.0.jar
echo -n "CWD is: "
echo `pwd`
echo `ls -l`

tar czf results.tar.gz kepler/kepler-1.0.0.jar
echo `ls -l`

echo "Finished NOOP script"
