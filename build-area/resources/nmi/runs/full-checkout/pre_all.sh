#! /bin/sh
# These commands are run after all of the input files have been fetched but
# before the platform-specific tasks are run
# It is executed from the common dir

# Move the R installers to the proper directory to build the installer
#mv R-2.6.2-win.zip kepler
#mv R-2.6.2-osx.dmg kepler
# copy the documentation from kepler-docs
#cp kepler-docs/outreach/documentation/shipping/*.* kepler/configs/ptolemy/configs/kepler/.
mv ptolemy kepler/modules/.
