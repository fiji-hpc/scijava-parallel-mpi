#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from com.mycompany.imagej import Measure

def fn():
    outputGPU = ops.run("CLIJ_create", input)
    inputGPU = ops.run("CLIJ_push", input)
    ops.run("CLIJ_maximumBox", outputGPU, inputGPU, 3, 3, 3)
    return ops.run("CLIJ_pull", outputGPU)

input = scifio.datasetIO().open(input_path)
output = Measure.benchmark(fn, rounds)
scifio.datasetIO().save(output, output_path)
print("OK")

