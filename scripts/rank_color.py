#@ OpService ops
#@ SCIFIO scifio
#@ DatasetService datasets
#@ String input_path
#@ String output_path
#@ UIService ui

import os
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.view import Views

try:
	os.unlink(output_path)
except OSError:
	pass

input = scifio.datasetIO().open(input_path)
	
# create result dataset
output = ops.create().img(input)

# color image with MPI ranks
ops.run('mpi.rankColor', output, input)

scifio.datasetIO().save(datasets.create(output), output_path)
ui.show(output)
print("OK")