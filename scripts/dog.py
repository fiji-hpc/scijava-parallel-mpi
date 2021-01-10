#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

from net.imglib2.type.numeric.real import FloatType
from net.imglib2.type.numeric.integer import UnsignedByteType
from net.imglib2.view import Views

input_dataset = scifio.datasetIO().open(input_path)

kernel_a =  ops.create().kernelGauss([0, 0])
a = ops.create().img(input_dataset)
ops.filter().convolve(a, Views.extendMirrorSingle(input_dataset), kernel_a)

kernel_b =  ops.create().kernelGauss([2, 2])
b = ops.create().img(input_dataset)
ops.filter().convolve(b, Views.extendMirrorSingle(input_dataset), kernel_b)

difference_of_gaussian = ops.math().subtract(a, b)
ui.show(input_dataset)
ui.show(a)
ui.show(b)
ui.show(difference_of_gaussian)