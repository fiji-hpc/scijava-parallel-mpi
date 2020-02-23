#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from net.imglib2.algorithm.neighborhood import RectangleShape
from cz.it4i.scijava.mpi import Measure
from cz.it4i.scijava.mpi import MPIUtils

def fn():
	return ops.run("mpi.transferBenchmark", input)

input = scifio.datasetIO().open(input_path)
output = Measure.benchmark(fn, rounds)
scifio.datasetIO().save(output, output_path)
print("OK")
