#! /bin/sh

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

#echo creating notify.nmi
#touch notify.nmi
#echo "testing notify.nmi" >> notify.nmi

#echo where are we?
#pwd

#echo what files are here?
#ls 

#echo "done"

echo processing kepler...
cd kepler/modules/build-area
echo Issuing change-to...attempting to checkout modules
ant change-to -Dsuite=kepler
echo attempting to compile...
ant compile >> ../../../notify.nmi
echo compile complete...starting unit tests.
ant test
echo unit tests complete.

# Package up installer as a results file that NMI will transfer to submit host
#echo -n "Creating results file in: "
#echo `pwd`
#tar czf results.tar.gz kepler-1.0.0.jar
#mv results.tar.gz ..
#cd ..
#echo -n "Moved results file to: "
#echo `pwd`
