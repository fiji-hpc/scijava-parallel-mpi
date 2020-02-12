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
	output = ops.create().img(input)
	ops.filter().max(output, input, RectangleShape(3, False))
	Measure.nextRound()
#ui.show(output)
print("OK")
