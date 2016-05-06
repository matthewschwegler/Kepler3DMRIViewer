#! /bin/sh
set -v on
date=`date +%Y-%m-%d-%H-%M`
echo "Starting REMOTE_POST"
echo -n "***Working directory is: "
echo `pwd`
file1=kepler-nightly-kepler-$date.tar.gz
tar -zcvf $file1 kepler-run
file2=kepler-nightly-kepler-osx-$date.jar
file3=kepler-nightly-kepler-win-$date.jar
file4=kepler-nightly-kepler-lin-$date.jar
file5=kepler-javadoc-$date.tar.gz
tar -zcvf $file5 kepler-run/modules/javadoc
cp kepler-run/modules/build-area/installer/kepler-nightly-kepler-osx.jar $file2
cp kepler-run/modules/build-area/installer/kepler-nightly-kepler-win.jar $file3
cp kepler-run/modules/build-area/installer/kepler-nightly-kepler-linux.jar $file4
tar -zcvf results.tar.gz $file1 $file2 $file3 $file4 $file5
echo "REMOTE_POST completed"
exit
