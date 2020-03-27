#!/bin/sh
tmpfile=$(mktemp)
trap 'rm -f $tmpfile' EXIT

cd figures || exit 1
for i in *.pdf; do
  pdfcrop "$i" "$tmpfile"
  mv "$tmpfile" "$i"
done
