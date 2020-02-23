#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds
#@ DatasetService datasetService

from cz.it4i.scijava.mpi import Measure

def fn():
    outputGPU = ops.run("CLIJ_create", input)
    inputGPU = ops.run("CLIJ_push", input)
    ops.run("CLIJ_minimumBox", outputGPU, inputGPU, 3, 3, 3)
    return ops.run("CLIJ_pull", outputGPU)

input = scifio.datasetIO().open(input_path)
output = Measure.benchmark(fn, rounds)
scifio.datasetIO().save(datasetService.create(output), output_path)
print("OK")
