#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from cz.it4i.scijava.mpi import Measure
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.view import Views

# create box-blur kernel
kernel = ops.run("create.img", [3, 3], FloatType())
for p in kernel:
	p.set(1.0/kernel.size())

def fn():
	output = ops.create().img(input)
	ops.filter().convolve(output, Views.extendMirrorSingle(input), kernel)
	return output

input = scifio.datasetIO().open(input_path)
output = Measure.benchmark(fn, rounds)
scifio.datasetIO().save(output, output_path)
print("OK")