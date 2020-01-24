package com.mycompany.imagej;

import com.mycompany.imagej.ops.MPIRankColor;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import static com.mycompany.imagej.Measure.measure;


public class Main {
    static double[][] boxBlurKernel(int size) {
        double [][] kernel = new double[size][size];
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                kernel[r][c] = 1.0 / (size * size);
            }
        }
//        kernel[size / 2][size / 2] = 1;
        return kernel;
    }

    static double[][] edgeKernel() {
        return new double[][]{
            new double[]{-1, -1, -1},
            new double[]{-1, 8, -1},
            new double[]{-1, -1, -1},
        };
    }

    public static void main(String[] args) {
        try {
            if(args.length == 0) {
                System.err.println("Missing operation argument");
                System.exit(1);
            }
            String op = args[0];
            String inputPath = args[1];
            String outputPath = args[2];

            ImageJ ij = new ImageJ();
            Dataset input = measure("read", () -> ij.scifio().datasetIO().open(inputPath));

            RandomAccessibleInterval output = ij.op().create().img(input);

            int rounds = 1;
            if(System.getenv("ROUNDS") != null) {
                rounds = Integer.parseInt(System.getenv("ROUNDS"));
            }
            for(int i = 0; i < rounds; i++) {
                if (op.equals("rank_color")) {
                    ij.op().run(MPIRankColor.class, output, input);
                } else if (op.equals("edge_convolution")) {
                    convolve(ij, output, (RandomAccessibleInterval) input, edgeKernel());
                } else if (op.equals("boxblur_convolution")) {
                    convolve(ij, output, (RandomAccessibleInterval) input, boxBlurKernel(30));
                } else {
                    System.err.println("Unknown op: " + op);
                    System.exit(1);
                }
                Measure.nextRound();
            }

            if (MPIUtils.isRoot()) {
                measure("write", () -> {
                    ij.scifio().datasetIO().save(ij.dataset().create(output), outputPath);
                });
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
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
