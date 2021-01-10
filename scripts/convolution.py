#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

import os
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.view import Views

try:
	os.unlink(output_path)
except OSError:
	pass


# create box-blur kernel
kernel = ops.run("create.img", [3, 3], FloatType())
for p in kernel:
	p.set(1.0/kernel.size())

input = scifio.datasetIO().open(input_path)

output = ops.create().img(input)
ops.filter().convolve(output, Views.extendMirrorSingle(input), kernel)

scifio.datasetIO().save(datasets.create(output), output_path)
ui.show(output)
print("OK")
