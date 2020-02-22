#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from cz.it4i.scijava.mpi import Measure

def fn():
	output = ops.create().img(input)
	scalar = output.firstElement().createVariable()
	scalar.setReal(100)
	ops.math().add(output, input, scalar)
	return output

input = scifio.datasetIO().open(input_path)
output = Measure.benchmark(fn, rounds)
scifio.datasetIO().save(output, output_path)
#ui.show(output)
print("OK")
