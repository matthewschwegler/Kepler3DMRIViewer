#! /bin/sh
set -v on
cd kepler-run/modules/build-area
ant change-to -Dsuite=wrp
ant compile
ant test
ant build-windows-installer -Dno-win-exe=true -DcopyDocumentation=false -Dreleasename=kepler-nightly-wrp
ant build-mac-installer -Dno-mac-app=true -DcopyDocumentation=false -Dreleasename=kepler-nightly-wrp
ant build-linux-installer -DcopyDocumentation=false -Dreleasename=kepler-nightly-wrp
