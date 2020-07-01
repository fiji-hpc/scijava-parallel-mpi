#!/bin/sh
tmpfile=$(mktemp)
trap 'rm -f $tmpfile' EXIT

cd figures || exit 1
find . -name "*.pdf" | while read i; do
  pdfcrop "$i" "$tmpfile"
  mv "$tmpfile" "$i"
done
