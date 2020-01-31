process() (
  dataset=$1
  echo "$dataset"
  mkdir "outputs/$dataset"
  cd "outputs/$dataset"

  export B_MAX_THREADS=1
  export B_KERNEL=identity
  export B_KERNEL_SIZE=1

  cp "../../dataset/$dataset" orig.tif
  convert "../../datasets/$dataset" gray:orig.raw
  orig_hash=$(md5sum orig.raw | cut -f 1 -d' ')

  for nodes in $(seq 1 8); do
    mpirun --bind-to none --oversubscribe -np "$nodes" java com.mycompany.imagej.Main convolution "../../datasets/$dataset" $nodes.tiff
    convert "$nodes.tiff" "gray:$nodes.raw"
    cur_hash=$(md5sum "$nodes.raw" | cut -f 1 -d' ')
    if [ "$cur_hash" != "$orig_hash" ]; then
      echo "$dataset with $nodes nodes has wrong checksum"
      exit 1
    fi
  done
  md5sum ./*.raw
)

rm -r outputs
mkdir outputs

for dataset in datasets/*.tif; do
  process $(basename "$dataset")
done
