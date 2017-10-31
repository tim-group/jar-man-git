#!/bin/bash

cd sbt-jarmangit
sbt-tg test publish

cd ../gradle-jarmangit
gradle -v
gradle -PCI --no-daemon clean build publish
