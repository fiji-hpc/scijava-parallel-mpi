#!/bin/sh
cd datasets
curl https://docs.openmicroscopy.org/ome-model/6.0.0/ome-tiff/data.html | grep -Eo 'http[^"]+\.tif' | sort | uniq | wget -i -
