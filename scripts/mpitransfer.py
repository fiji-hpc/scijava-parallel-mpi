#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

import os

try:
	os.unlink(output_path)
except OSError:
	pass

input = scifio.datasetIO().open(input_path)
output = ops.run("mpi.transferBenchmark", input)
scifio.datasetIO().save(datasets.create(output), output_path)
ui.show(output)
print("OK")
