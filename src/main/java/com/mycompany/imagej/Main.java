package com.mycompany.imagej;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;


public class Main {
    static double[][] identityKernel(int size) {
        double [][] kernel = new double[size][size];
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                kernel[r][c] = 0;
            }
        }
        kernel[size / 2][size / 2] = 1;
        return kernel;
    }

    static double[][] edgeKernel() {
        return new double[][]{
            new double[]{-1, -1, -1},
            new double[]{-1, 8, -1},
            new double[]{-1, -1, -1},
        };
    }

    public static void main(String[] args) throws Exception{
        ImageJ ij = new ImageJ();
        Dataset input =  ij.scifio().datasetIO().open(args[0]);
        RandomAccessibleInterval output = ij.op().create().img(input);

//        ij.op().run(MPIRankColor.class, output, input);
//        convolve(ij, output, (RandomAccessibleInterval) input, edgeKernel());
        convolve(ij, output, (RandomAccessibleInterval) input, identityKernel(3));

        if(MPIUtils.isRoot()) {
            ij.scifio().datasetIO().save(ij.dataset().create(output), "result.tif");
        }

        System.exit(0);
    }

    private static <O extends RealType<O>> void convolve(ImageJ ij, RandomAccessibleInterval<O> output, RandomAccessibleInterval<O> input, double[][] kernel) {
        ij.op().filter().convolve(
                output,
                Views.extendMirrorSingle(input),
                ij.op().create().kernel(kernel, new DoubleType())
        );
    }

}
