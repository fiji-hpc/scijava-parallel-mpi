#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from cz.it4i.scijava.mpi import Measure
from net.imglib2.type.numeric.integer import UnsignedByteType
from net.imglib2.type.logic import BitType
from net.imglib2.view import Views

def fn():
	output = ops.create().img(input, BitType())
	threshold = UnsignedByteType(128)
	ops.threshold().apply(output, input, threshold)
	return output

input = scifio.datasetIO().open(input_path)
output = Measure.benchmark(fn, rounds)

output8bit = ops.convert().uint8(output)
output8bit = ops.math().multiply(output8bit, UnsignedByteType(255))

scifio.datasetIO().save(output8bit, output_path)
#ui.show(output8bit)
print("OK")
