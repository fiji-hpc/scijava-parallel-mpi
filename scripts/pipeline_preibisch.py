#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets
#@ Double (value=3, persist=false) sigma
#@ Double (value=0.0005, persist=false) low_threshold
#@ Double (value=0.001, persist=false) high_threshold

print(sigma, low_threshold, high_threshold)

from cz.it4i.scijava.mpi import Measure
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.type.numeric.integer import UnsignedByteType
from net.imglib2.view import Views
from io.scif.config import SCIFIOConfig
from io.scif.formats.tiff import IFD

start = Measure.start("total_op")
input_dataset = scifio.datasetIO().open(input_path)
input_dataset = ops.convert().float32(input_dataset)
preprocess = ops.create().img(input_dataset)
minMax = ops.stats().minMax(input_dataset)
input_dataset = ops.image().normalize(preprocess, input_dataset, minMax.getA(), minMax.getB(), FloatType(0), FloatType(1.0))
#ui.show(preprocess)

gauss_kernel =  ops.create().kernelGauss([sigma, sigma])
print(gauss_kernel)

without_noise = ops.create().img(input_dataset)
ops.filter().convolve(without_noise, Views.extendMirrorSingle(input_dataset), gauss_kernel)
input_dataset = None
#ui.show(without_noise)

edges = ops.run("edgeDetector", without_noise, low_threshold, high_threshold)
#ui.show(edges)
config = SCIFIOConfig().writerSetFailIfOverwriting(False)
config[IFD.BIG_TIFF] = True
scifio.datasetIO().save(datasets.create(ops.convert().uint8(ops.math().multiply(edges, FloatType(255)))), output_path, config)

Measure.end(start)
print("OK")
