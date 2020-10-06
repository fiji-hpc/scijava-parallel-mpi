#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

from cz.it4i.scijava.mpi import Measure
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.type.numeric.integer import UnsignedByteType
from net.imglib2.view import Views

start = Measure.start("total_op")
input_dataset = scifio.datasetIO().open(input_path)
input_dataset = ops.convert().float32(input_dataset)
preprocess = ops.create().img(input_dataset)
minMax = ops.stats().minMax(input_dataset)
input_dataset = ops.image().normalize(preprocess, input_dataset, minMax.getA(), minMax.getB(), FloatType(0), FloatType(1.0))
#ui.show(preprocess)

gauss_kernel =  ops.create().kernelGauss([3, 3])
print(gauss_kernel)

without_noise = ops.create().img(input_dataset)
ops.filter().convolve(without_noise, Views.extendMirrorSingle(input_dataset), gauss_kernel)
input_dataset = None
#ui.show(without_noise)

edges = ops.run("edgeDetector", without_noise, 0.0008, 0.001)
#ui.show(edges)
scifio.datasetIO().save(datasets.create(ops.convert().uint8(ops.math().multiply(edges, 255))), output_path)

Measure.end(start)
print("OK")
