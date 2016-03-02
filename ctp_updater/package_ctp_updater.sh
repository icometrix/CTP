#!/bin/bash
set -e
if [ "$#" -ne 2 ]; then
  echo "------------------------------------Expecting user and password..."
  exit 1
fi
echo "------------------------------------Authenticating...."
curl -c cookie.txt --fail -X POST -u $1:$2 https://customers-sessions.icometrix.com/sessions
#some parsing of the cookie
sed -i.bak 's/customers-sessions.icometrix.com/dicom-router.icometrix.com/g' cookie.txt
rm cookie.txt.bak
echo "------------------------------------Assuming to be in ctp_updater...";
cd ../
export CURRENT_TAG=$(git describe --tags);
export CTP_HOME=$(pwd);
export CTP_UPDATER=$CTP_HOME/ctp_updater;
echo "------------------------------------Start build";
ant;
echo "------------------------------------Copying build files to $CTP_UPDATER/ctp_files";
mkdir -p $CTP_UPDATER/ctp_files
cp -r $CTP_HOME/build/CTP/* $CTP_UPDATER/ctp_files;
echo "------------------------------------Now packaging evertyhing in a nice zip";
cd $CTP_UPDATER;
export ZIP_FILE=CtpUpdater.zip;
rm -rf CtpUpdater.jar;
rm -rf $ZIP_FILE;
ant;
zip -r $ZIP_FILE CtpUpdater.jar ctp_control_scripts ctp_files log4j.properties
echo "------------------------------------Hola, la construccion esta terminado!";
echo "------------------------------------Now, moving the package to the release server"
curl -v -b cookie.txt --fail -X PUT -F upload=@$ZIP_FILE https://dicom-router.icometrix.com/api/v1/releases/$CURRENT_TAG;
echo "------------------------------------Pushing to server success!"
echo "------------------------------------Cleaning up!"
rm cookie.txt;
echo "------------------------------------Done!"
