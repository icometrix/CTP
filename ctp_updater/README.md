#Module: ctp_updater
Module responsible for deploying an update

##Folder structure:
some files are not included in the .jar.
These should be present in your (eclipse) workspace, if you want to start debugging.

* ctp_controller_scripts
* log4j.properties
* package_ctp_updater.sh


##Building
as usual:
ant;

##Packaging and pushing to the release center
just run
build_updater.sh

