#!/bin/sh
convert -density 300 "$1" png:- | tesseract stdin stdout -l deu
