#@ OpService ops
#@ SCIFIO scifio
#@ DatasetService datasets
#@ String input_path
#@ String output_path


from net.imglib2.type.numeric.real import FloatType
from net.imglib2.view import Views

print("input_path: " + input_path)
input = scifio.datasetIO().open(input_path)
	
# create result dataset
output = ops.create().img(input)

# color image with MPI ranks
ops.run('mpi.rankColor', output, input)

scifio.datasetIO().save(datasets.create(output), output_path)
print("OK")