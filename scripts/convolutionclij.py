#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ int rounds

from net.imglib2.type.numeric.real import FloatType

input = scifio.datasetIO().open(input_path)

# create box-blur kernel
kernel = ops.run("create.img", [3, 3], FloatType())
for p in kernel:
	p.set(1.0/kernel.size())
	
kernelGPU = ops.run("CLIJ_push", kernel)
outputGPU = ops.run("CLIJ_create", input)
inputGPU = ops.run("CLIJ_push", input)
ops.run("filter.convolve", inputGPU, kernelGPU, outputGPU)
output = ops.run("CLIJ_pull", outputGPU)


ui.show(output)