#! /bin/bash

. functions.sh

processArgs $*

# Delete old logs
rm -f $LOGDIR/*.log

startKernel --autorun --nomenu --nogui --nolog
#startSims --nogui --viewer.team-name=Sample --viewer.maximise=true

#makeClasspath $BASEDIR/lib
#xterm -T agents -e "./sampleagent.sh" &
#PIDS="$PIDS $!"

#waitFor $LOGDIR/kernel.log "Kernel has shut down" 30

#kill $PIDS

# usage
# ./demo.sh -c ../../scenarios/robocup2019/test/config -m ../../scenarios/robocup2019/test/map -t ait
