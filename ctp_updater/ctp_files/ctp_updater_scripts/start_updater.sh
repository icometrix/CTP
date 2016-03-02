#!/bin/bash
# $1 = rootUpdater
# $2 = rootCtp
# $3 = portCtp
# $4 = sslCtp
# $5 = lockFileCtpUpdater
cd $1;
echo "Running command";
echo "java -jar CtpUpdater.jar $2 $3 $4 $5"
java -jar CtpUpdater.jar $2 $3 $4 $5;
