#!/bin/sh
# $Id$
# A simple smoke test for Kepler.
# Start up the Xserver, run Kepler, then kill the Xserver.
# This should be run from the build-area directory.
# This test is linux/unix-specific,
# it uses the Xvfb virtual frame buffer and the timeout command.
#
# This test is run as part of the nightly build at Berkeley.

DISPLAY=8
echo "Killing Xvfb"
pkill Xvfb
pkill -9 Xvfb
rm -f /tmp/.X${DISPLAY}-lock

echo "Starting Xvfb"
Xvfb :${DISPLAY} -screen 0 1024x768x24 &
export DISPLAY=localhost:${DISPLAY}.0

timeout=30
echo "Displaying the version with a timeout of $timeouts seconds"

# ConfigurationApplication defines various command line arguments.
# The timeout command is probably Linux-specific.  It returns 124 
# if the ant command does not exit within $timeout seconds
timeout $timeout ant run -Dargs=-version
status=$?
echo "Status was: $status"

echo "Killing Xvfb"
pkill Xvfb
pkill -9 Xvfb
rm -f /tmp/.X${DISPLAY}-lock

exit $status
