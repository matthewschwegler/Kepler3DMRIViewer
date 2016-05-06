#! /bin/sh
set -v on
date=`date +%Y-%m-%d-%H-%M`
echo "Starting REMOTE_POST"
echo -n "***Working directory is: "
echo `pwd`
echo '  ***files here:'
ls
echo '  ***files in kepler-run/modules/build-area/installer:'
ls kepler-run/modules/build-area/installer


echo '***tarring up the build***'
file1=kepler-nightly-$date.tar.gz
tar -zcvf $file1 kepler-run

echo '\n***getting installers from the results file***\n'
file2=kepler-1.xDev-osx-$date.jar
file3=kepler-1.xDev-win-$date.jar
cp kepler-run/modules/build-area/installer/kepler-1.xDev-osx.jar $file2
cp kepler-run/modules/build-area/installer/kepler-1.xDev-win.jar $file3
tar -zcvf results.tar.gz $file1 $file2 $file3

echo "REMOTE_POST completed"
exit
