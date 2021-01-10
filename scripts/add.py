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
output = ops.create().img(input)


scalar = output.firstElement().createVariable()
scalar.setReal(100)
ops.math().add(output, input, scalar)

scifio.datasetIO().save(datasets.create(output), output_path)
ui.show(output)
print("OK")
