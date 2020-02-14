#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from com.mycompany.imagej import Measure

def fn():
	return ops.stats().minMax(input)

input = scifio.datasetIO().open(input_path)
output = Measure.benchmark(fn, rounds)
print("Min: {}, Max: {}".format(output.getA(), output.getB()))
print("OK")