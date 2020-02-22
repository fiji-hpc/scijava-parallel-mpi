#@ OpService ops
#@ SCIFIO scifio
#@ String input_path

import os
from net.imglib2.type.numeric.real import FloatType
from net.imglib2.view import Views

print("input_path: " + input_path)
input = scifio.datasetIO().open(input_path)
	
# create result dataset
output = ops.create().img(input)

# color image with MPI ranks
ops.run('mpi.rankColor', output, input)

output_path = os.path.join(os.getcwd(), "output" + os.path.splitext(input_path)[-1])
scifio.datasetIO().save(output, output_path)
print("Output saved to: " + output_path)