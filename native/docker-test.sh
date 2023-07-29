#!/bin/bash

docker build . -t josm/josm
mkdir -p test/report
docker run -it --name josm -v "$(pwd)"/test/report:/josm/test/report josm/josm
docker rm josm
docker rmi josm/josm
