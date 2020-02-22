# ~/Fiji.app/ImageJ-linux64  -- --ij2 --headless --run ./scripts/stats.py "input_path=\"./run/datasets/lena_gray_8.tif\"" > /tmp/mpi
# B_NO_MPI_OPS=1 ~/Fiji.app/ImageJ-linux64  -- --ij2 --headless --run ./scripts/stats.py "input_path=\"./run/datasets/lena_gray_8.tif\"" > /tmp/default
# diff <(grep -E "^[a-zA-Z0-9]{3,}:" /tmp/mpi) <(grep -E "^[a-zA-Z0-9]{3,}:" /tmp/default) -y

#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ UIService ui

input = scifio.datasetIO().open(input_path)

print("geometricMean: {}\n").format(ops.stats().geometricMean(input))
print("harmonicMean: {}\n").format(ops.stats().harmonicMean(input))
#print("integralMean: {}\n").format(ops.stats().integralMean(input))
#print("integralSum: {}\n").format(ops.stats().integralSum(input))
#print("integralVariance: {}\n").format(ops.stats().integralVariance(input))
print("kurtosis: {}\n").format(ops.stats().kurtosis(input))
print("max: {}\n").format(ops.stats().max(input))
print("mean: {}\n").format(ops.stats().mean(input))
#print("median: {}\n").format(ops.stats().median(input))
print("min: {}\n").format(ops.stats().min(input))
minMax = ops.stats().minMax(input)
print("minMax: min={} max={}\n").format(minMax.getA(), minMax.getB())
print("moment1AboutMean: {}\n").format(ops.stats().moment1AboutMean(input))
print("moment2AboutMean: {}\n").format(ops.stats().moment2AboutMean(input))
print("moment3AboutMean: {}\n").format(ops.stats().moment3AboutMean(input))
print("moment4AboutMean: {}\n").format(ops.stats().moment4AboutMean(input))
#print("percentile: {}\n").format(ops.stats().percentile(input, 50))
#print("quantile: {}\n").format(ops.stats().quantile(input, 0.5))
print("skewness: {}\n").format(ops.stats().skewness(input))
print("stdDev: {}\n").format(ops.stats().stdDev(input))
print("sum: {}\n").format(ops.stats().sum(input))
print("sumOfInverses: {}\n").format(ops.stats().sumOfInverses(input))
print("sumOfLogs: {}\n").format(ops.stats().sumOfLogs(input))
print("sumOfSquares: {}\n").format(ops.stats().sumOfSquares(input))
print("variance: {}\n").format(ops.stats().variance(input))

print("OK")
