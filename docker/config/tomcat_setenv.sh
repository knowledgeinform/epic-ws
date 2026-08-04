#!/bin/sh

# Heap (be sure your container has enough RAM for this)
CATALINA_OPTS="$CATALINA_OPTS -Xms8g -Xmx8g"

# Use G1GC
CATALINA_OPTS="$CATALINA_OPTS -XX:+UseG1GC"
CATALINA_OPTS="$CATALINA_OPTS -XX:MaxGCPauseMillis=200"

# Start concurrent marking earlier
CATALINA_OPTS="$CATALINA_OPTS -XX:InitiatingHeapOccupancyPercent=30"

# Keep some free space to reduce evacuation failures
CATALINA_OPTS="$CATALINA_OPTS -XX:G1ReservePercent=20"

# Helpful with reference processing overhead
CATALINA_OPTS="$CATALINA_OPTS -XX:+ParallelRefProcEnabled"

# String deduplication
CATALINA_OPTS="$CATALINA_OPTS -XX:+UseStringDeduplication"

# Diagnostics: heap dump and exit on OOM
CATALINA_OPTS="$CATALINA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/logs"
CATALINA_OPTS="$CATALINA_OPTS -XX:+ExitOnOutOfMemoryError"

# GC logs (Java 8 style)
CATALINA_OPTS="$CATALINA_OPTS -Xloggc:/usr/local/tomcat/logs/gc.log"
CATALINA_OPTS="$CATALINA_OPTS -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
CATALINA_OPTS="$CATALINA_OPTS -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=20M"

export CATALINA_OPTS
