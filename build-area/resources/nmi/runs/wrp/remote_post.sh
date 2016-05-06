#! /bin/sh
set -v on
date=`date +%Y-%m-%d-%H-%M`
echo "Starting REMOTE_POST"
echo -n "***Working directory is: "
echo `pwd`
file1=kepler-nightly-wrp-$date.tar.gz
tar -zcvf $file1 kepler-run
file2=kepler-nightly-wrp-osx-$date.jar
file3=kepler-nightly-wrp-win-$date.jar
file4=kepler-nightly-wrp-lin-$date.jar
cp kepler-run/modules/build-area/installer/kepler-nightly-wrp-osx.jar $file2
cp kepler-run/modules/build-area/installer/kepler-nightly-wrp-win.jar $file3
cp kepler-run/modules/build-area/installer/kepler-nightly-wrp-linux.jar $file4
tar -zcvf results.tar.gz $file1 $file2 $file3 $file4
echo "REMOTE_POST completed"
exit
