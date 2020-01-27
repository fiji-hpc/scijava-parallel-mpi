# mpirun -np 2 ~/Fiji.app/ImageJ-linux64 --ij2 --headless --run ./scripts/convolution.py 'input_path="./run/datasets/lena_gray_8.tif"'

#@ OpService ops
#@ SCIFIO scifio
#@ String input_path

import os
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.view import Views

print("input_path: " + input_path)
input = scifio.datasetIO().open(input_path)

# create box-blur kernel
kernel = ops.run("create.img", [3, 3], FloatType())
for p in kernel:
	p.set(1.0/kernel.size())
	
# create result dataset
output = ops.create().img(input)

# make a convolution with our kernel
ops.filter().convolve(output, Views.extendMirrorSingle(input), kernel)

output_path = os.path.join(os.getcwd(), "output" + os.path.splitext(input_path)[-1])
scifio.datasetIO().save(output, output_path)
print("Output saved to: " + output_path)