#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

import os
from net.imglib2.type.numeric.integer import UnsignedByteType
from net.imglib2.type.logic import BitType
from net.imglib2.view import Views

try:
	os.unlink(output_path)
except OSError:
	pass

input = scifio.datasetIO().open(input_path)
output = ops.create().img(input, BitType())
threshold = UnsignedByteType(128)
ops.threshold().apply(output, input, threshold)

output8bit = ops.convert().uint8(output)
output8bit = ops.math().multiply(output8bit, UnsignedByteType(255))

scifio.datasetIO().save(datasets.create(output8bit), output_path)
ui.show(output8bit)
print("OK")
