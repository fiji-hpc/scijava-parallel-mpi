#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ UIService ui
#@ int rounds

from net.imglib2.algorithm.neighborhood import RectangleShape
from com.mycompany.imagej import Measure

input = scifio.datasetIO().open(input_path)

for i in range(0, rounds):
    print("RUN {}".format(i))
    outputGPU = ops.run("CLIJ_create", input)
    inputGPU = ops.run("CLIJ_push", input)
    ops.run("CLIJ_maximumBox", outputGPU, inputGPU, 3, 3, 3)
    output = ops.run("CLIJ_pull", outputGPU)
    #ui.show(output)
    Measure.nextRound()
print("OK")

