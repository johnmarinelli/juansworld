#!/bin/bash

lein do clean
lein cljsbuild once min
open resources/public/index.html

function upload_to_s3 () {
  aws s3 cp resources/public/index.html s3://juans.world/
  aws s3 cp resources/public/js/compiled/juansworld.js s3://juans.world/js/compiled/
  aws s3 cp resources/public/css/style.css s3://juans.world/css/
}

while true; do 
  read -p "opening static site in browser.  does it look good? " yn

  case $yn in 
    [Yy]* ) upload_to_s3; break;;
    [Nn]* ) exit;;
    *     ) echo "answer y/n"
  esac
done
