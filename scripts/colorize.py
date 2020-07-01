#@ OpService ops
#@ SCIFIO scifio
#@ String input_path
#@ String output_path
#@ UIService ui
#@ DatasetService datasets

from cz.it4i.scijava.mpi import Measure
from net.imglib2.type.numeric.real import DoubleType
from net.imglib2.view import Views
from net.imagej import Dataset
from net.imagej.display import ColorTables
from net.imagej.axis import Axes

rgb = scifio.datasetIO().open(input_path)

colorized = ops.run("mpi.rankColor", rgb)

d = datasets.create(colorized)
d.initializeColorTables(3)
d.setColorTable(ColorTables.RED, 0)
d.setColorTable(ColorTables.GREEN, 1)
d.setColorTable(ColorTables.BLUE, 2)
d.setCompositeChannelCount(3)
d.axis(2).setType(Axes.CHANNEL)
d.setRGBMerged(True)

#ui.show(d)
scifio.datasetIO().save(d, output_path)
print("OK")