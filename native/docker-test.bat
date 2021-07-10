docker build . -t josm/josm
mkdir %CD%\test\report
docker run -it --name josm -v %CD%\test\report:/josm/test/report josm/josm
docker rm josm
docker rmi josm/josm
pause
