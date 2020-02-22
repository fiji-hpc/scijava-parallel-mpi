#@ OpService ops
#@ SCIFIO scifio
#@ DatasetService datasetService
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from cz.it4i.scijava.mpi import Measure
from net.imglib2.util import Intervals
from net.haesleinhuepf.clij.coremem.enums import NativeTypeEnum

def fn():
	output_dims = dims[:-1]
	outputGPU = ops.run("CLIJ_create", output_dims, NativeTypeEnum.UnsignedByte)
	inputGPU = ops.run("CLIJ_push", input.getImgPlus().getImg())
	ops.run('CLIJ_maximumZProjection', outputGPU, inputGPU)
	return ops.run("CLIJ_pull", outputGPU)

input = scifio.datasetIO().open(input_path)
dims = Intervals.dimensionsAsLongArray(input)
output = Measure.benchmark(fn, rounds)
scifio.datasetIO().save(datasetService.create(output), output_path)
#ui.show(output)
print("OK")