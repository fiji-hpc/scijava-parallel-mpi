# imagej_openmpi

## Benchmarking
All data are stored in `./run` directory. This directory should be linked to the scratch fs.
Input data are stored in `./run/datasets`, outputs are saved to `./run/outputs`

```
$ source env.sh
$ B_THREADS_NUM=1 B_KERNEL_SIZE=3 B_KERNEL=edge B_ROUNDS=5 B_MAX_NODES=8 \
  benchrun convolution world_21600x21600_8.tif
$ benchpostprocess # computes output checksums, generates result thumbnails
$ benchreports # generates html reports
```
