#!/usr/bin/env bash

./gradlew clean installDist distZip
echo Zip file built in build/distributions
echo Start script built in build/install/book/bin/book
