#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui

input = scifio.datasetIO().open(input_path)
output = ops.stats().minMax(input)
print("Min: {}, Max: {}".format(output.getA(), output.getB()))
print("OK")