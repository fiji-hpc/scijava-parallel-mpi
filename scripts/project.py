#@ OpService ops
#@ SCIFIO scifio
#@ DatasetService datasetService
#@ String input_path
#@ String output_path
#@ UIService ui

import os
from net.imglib2.util import Intervals
from net.imglib2.view import IterableRandomAccessibleInterval

try:
	os.unlink(output_path)
except OSError:
	pass

input = scifio.datasetIO().open(input_path)
dims = Intervals.dimensionsAsLongArray(input)
output_dims = dims[:-1]
output = ops.create().img(output_dims)
ops.transform().project(
        IterableRandomAccessibleInterval(output),
        input, 
        ops.op('stats.max', input.getImgPlus()),
        len(output_dims)
)

scifio.datasetIO().save(datasetService.create(output), output_path)
ui.show(output)
print("OK")