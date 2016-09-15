#!/bin/sh

V4L2_CTL=$(which v4l2-ctl)
if [ -x "$V4L2_CTL" ]; then
	# TODO: run proper v4l2 ctl commands
	$V4L2_CTL -c auto_exposure=1 -c exposure_time_absolute=3
else
	echo "v4l2-ctl is not in the path. Not configuring camera"
fi;