#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasetService
#@ int rounds

from cz.it4i.scijava.mpi import Measure

def fn():
	outputGPU = ops.run("CLIJ_create", input)
	inputGPU = ops.run("CLIJ_push", input)
	ops.run('CLIJ_addImageAndScalar', outputGPU, inputGPU, 100.0)
	return ops.run("CLIJ_pull", outputGPU)

input = scifio.datasetIO().open(input_path)
output = Measure.benchmark(fn, rounds)
scifio.datasetIO().save(datasetService.create(output), output_path)
#ui.show(output)
print("OK")
