#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

from net.imglib2.type.numeric.integer import UnsignedByteType
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.algorithm.neighborhood import RectangleShape
from net.imglib2.type.logic import BitType
from net.imglib2.view import Views
from net.imglib2.util import Intervals
from net.imglib2.view import IterableRandomAccessibleInterval
from net.imagej.ops.stats import IterableMax

input = scifio.datasetIO().open(input_path)

# add constant
input = ops.math().add(input, UnsignedByteType(100))

# normalization
normalized = ops.create().img(input)
input = ops.image().normalize(normalized, input)

# convolution
kernel = ops.run("create.img", [3, 3], FloatType())
for p in kernel:
	p.set(1.0/kernel.size())
convoluted = ops.create().img(input)
ops.filter().convolve(convoluted, Views.extendMirrorSingle(normalized), kernel)

# threshold
thresholded = ops.create().img(input, BitType())
ops.threshold().apply(thresholded, convoluted, UnsignedByteType(128))


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

# min filter
#output = ops.create().img(input)
#ops.filter().min(output, input, RectangleShape(3, False))


#ui.show(projected)
#scifio.datasetIO().save(d, output_path)
print("OK")