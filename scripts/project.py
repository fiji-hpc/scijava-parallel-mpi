#@ OpService ops
#@ SCIFIO scifio
#@ DatasetService datasetService
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from cz.it4i.scijava.mpi import Measure
from net.imglib2.util import Intervals
from net.imglib2.view import IterableRandomAccessibleInterval

def fn():
        output_dims = dims[:-1]
        output = ops.create().img(output_dims)
        
        return ops.transform().project(
                IterableRandomAccessibleInterval(output),
                input, 
                ops.op('stats.max', input.getImgPlus()),
                len(output_dims)
        )

input = scifio.datasetIO().open(input_path)
dims = Intervals.dimensionsAsLongArray(input)

output = Measure.benchmark(fn, rounds)
scifio.datasetIO().save(datasetService.create(output), output_path)
#ui.show(output)
print("OK")
