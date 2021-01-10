#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

import os
from net.imglib2.algorithm.neighborhood import RectangleShape

try:
	os.unlink(output_path)
except OSError:
	pass

input = scifio.datasetIO().open(input_path)
output = ops.create().img(input)
ops.filter().min(output, input, RectangleShape(3, False))
scifio.datasetIO().save(datasets.create(output), output_path)
ui.show(output)
print("OK")
