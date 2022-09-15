#!/usr/bin/env bash

# JVM Heap, stack size and GC options
#export JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75 -XX:MaxDirectMemorySize=10M -XX:MaxMetaspaceSize=580078K -XX:ReservedCodeCacheSize=240M -Xss1M -Xmx3096081K"

#
# Enable remote debugging
#export REMOTE_DEBUGGER=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=4000

#
# Print JVM memory details when process exit
#export NATIVE_MEMORY_TRACKER="-XX:+UnlockDiagnosticVMOptions -XX:NativeMemoryTracking=summary -XX:+PrintNMTStatistics"

#rm -rf data/* || exit 1
#rm -rf db/data-storage.db || exit 1
java ${REMOTE_DEBUGGER} ${JAVA_OPTS} ${NATIVE_MEMORY_TRACKER} -jar target/data-node-agent.jar
