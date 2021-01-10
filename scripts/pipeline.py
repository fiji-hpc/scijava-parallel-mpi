#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

import os
from net.imglib2.type.numeric.integer import UnsignedByteType
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.type.logic import BitType
from net.imglib2.view import Views
from net.imglib2.util import Intervals
from net.imglib2.view import IterableRandomAccessibleInterval
from net.imagej.ops.stats import IterableMax
from net.imglib2.algorithm.neighborhood import RectangleShape

try:
	os.unlink(output_path)
except OSError:
	pass

input = scifio.datasetIO().open(input_path)
ui.show(input)

# add constant
input = ops.math().add(input, UnsignedByteType(100))
ui.show(input)

# normalization
normalized = ops.create().img(input)
ops.image().normalize(normalized, input)
ui.show(normalized)

# convolution
kernel = ops.run("create.img", [3, 3], FloatType())
for p in kernel:
	p.set(1.0/kernel.size())
convoluted = ops.create().img(normalized)
ops.filter().convolve(convoluted, Views.extendMirrorSingle(normalized), kernel)
ui.show(convoluted)

# min filter
minfiltered = ops.create().img(convoluted)
ops.filter().min(minfiltered, input, RectangleShape(3, False))
ui.show(minfiltered)

# threshold
thresholded = ops.create().img(minfiltered, BitType())
ops.threshold().apply(thresholded, minfiltered, UnsignedByteType(128))
ui.show(thresholded)

# project
dims = Intervals.dimensionsAsLongArray(thresholded)
projected_dims = dims[:-1]
projected = ops.create().img(projected_dims)
ops.transform().project(
   IterableRandomAccessibleInterval(projected),
   thresholded, 
   ops.op(IterableMax, input),
   len(projected_dims)
)
ui.show(projected)

scifio.datasetIO().save(datasets.create(projected), output_path)
print("OK")