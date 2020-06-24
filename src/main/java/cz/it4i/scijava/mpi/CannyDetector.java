package cz.it4i.scijava.mpi;

import cz.it4i.scijava.mpi.ops.convert.RGB2Grayscale;
import cz.it4i.scijava.mpi.ops.edgeDetector.EdgeDetector;
import io.scif.SCIFIO;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IterableRandomAccessibleInterval;

public class CannyDetector {
    public static void main(String[] args) throws Exception {
        ImageJ ij = new ImageJ();
        OpService ops = ij.op();
        SCIFIO scifio = ij.scifio();

        // Load dataset
        Dataset rgb = scifio.datasetIO().open(args[0]);

        // Load and convert to grayscale
        long start = Measure.start();
        RandomAccessibleInterval img = (RandomAccessibleInterval) ops.run(RGB2Grayscale.class, rgb);
        Measure.end("grayscale2rgb", start);
        rgb = null;


        // Convert to float64 type - double
        start = Measure.start();
        img = ops.convert().float64((IterableInterval) img);
        Measure.end("float64", start);

        // Remove a noise
        RandomAccessibleInterval withoutNoise = ops.create().img(img);
        start = Measure.start();
        ops.filter().convolve(withoutNoise, img, (RandomAccessibleInterval) ops.create().kernel(new double[][]{
                    new double[]{1 / 256.0, 4 / 256.0, 6 / 256.0, 4 / 256.0, 1 / 256.0},
                    new double[]{4 / 256.0, 14 / 256.0, 24 / 256.0, 14 / 256.0, 4 / 256.0},
                    new double[]{6 / 256.0, 24 / 256.0, 36 / 256.0, 24 / 256.0, 6 / 256.0},
                    new double[]{4 / 256.0, 14 / 256.0, 24 / 256.0, 14 / 256.0, 4 / 256.0},
                    new double[]{1 / 256.0, 4 / 256.0, 6 / 256.0, 4 / 256.0, 1 / 256.0}
                }, new DoubleType()
        ));
        Measure.end("gauss_blur", start);

        RandomAccessibleInterval<DoubleType> edges = (RandomAccessibleInterval<DoubleType>) ops.run(EdgeDetector.class, withoutNoise);
        ij.ui().show(edges);
        ij.scifio().datasetIO().save(ij.dataset().create(ij.op().convert().uint8(ops.math().multiply(new IterableRandomAccessibleInterval<DoubleType>(edges), new DoubleType(255)))), "/tmp/a.tif");
    }
}
