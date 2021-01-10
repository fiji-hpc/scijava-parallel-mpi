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

rgb = scifio.datasetIO().open(input_path)

grayscale = ops.run("convert.rgb2grayscale", rgb)
grayscale = ops.convert().float32(grayscale)
rgb = None

gauss_kernel = ops.create().kernel([
	[1 / 256.0, 4 / 256.0, 6 / 256.0, 4 / 256.0, 1 / 256.0],
	[4 / 256.0, 14 / 256.0, 24 / 256.0, 14 / 256.0, 4 / 256.0],
	[6 / 256.0, 24 / 256.0, 36 / 256.0, 24 / 256.0, 6 / 256.0],
	[4 / 256.0, 14 / 256.0, 24 / 256.0, 14 / 256.0, 4 / 256.0],
	[1 / 256.0, 4 / 256.0, 6 / 256.0, 4 / 256.0, 1 / 256.0]
], FloatType())

without_noise = ops.create().img(grayscale)
ops.filter().convolve(without_noise, Views.extendMirrorSingle(grayscale), gauss_kernel)

edges = ops.run("edgeDetector", without_noise)
scifio.datasetIO().save(datasets.create(ops.convert().uint8(ops.math().multiply(edges, 255))), output_path)
ui.show(edges)
print("OK")