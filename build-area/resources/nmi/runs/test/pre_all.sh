#! /bin/sh
set -v on
echo "creating directory structure"
mkdir kepler-run
mkdir kepler-run/modules

echo "moving modules into kepler-run directory"
mv actors kepler-run/modules/.
mv apple-extensions kepler-run/modules/.
mv common kepler-run/modules/.
mv core kepler-run/modules/.
mv directors kepler-run/modules/.
mv event-state kepler-run/modules/.
mv gui kepler-run/modules/.
mv loader kepler-run/modules/.
mv module-manager kepler-run/modules/.
mv ptII kepler-run/modules/.
mv ptolemy-lib kepler-run/modules/.
mv ptolemy kepler-run/modules/.
mv r kepler-run/modules/.
mv util kepler-run/modules/.
mv build-area kepler-run/modules/.
mv kepler kepler-run/modules/.

echo "deleting ptolemy svn files with long names"
#if you don't do this, long filename in windows will cause the windows build to fail
rm -rf `find . -type d -name .svn`
