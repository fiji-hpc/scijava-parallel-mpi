package com.mycompany.imagej;

import com.mycompany.imagej.ops.MPIRankColor;
import io.scif.config.SCIFIOConfig;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.convert.clip.ClipRealTypes;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IterableRandomAccessibleInterval;
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
            Dataset input = measure("read", () -> ij.scifio().datasetIO().open(inputPath, createSCIFIOConfig()));
            Utils.rootPrint("Input image: " + input.getImgPlus().getImg());

            RandomAccessibleInterval output = ij.op().create().img(input, new UnsignedByteType());

            int rounds = 1;
            if(System.getenv("B_ROUNDS") != null) {
                rounds = Integer.parseInt(System.getenv("B_ROUNDS"));
            }
            for(int i = 0; i < rounds; i++) {
                if (op.equals("rank_color")) {
                    ij.op().run(MPIRankColor.class, output, input);
                } else if (op.equals("edge_convolution")) {
                    RandomAccessibleInterval<FloatType> result;
/*
                    result = (RandomAccessibleInterval<FloatType>) ij.op().run(
                            ConvolveFFTF.class,
                            (RandomAccessibleInterval<UnsignedByteType>) input.getImgPlus().getImg(),
                            ij.op().create().kernel(edgeKernel(), new DoubleType()),
                            null,
                            new OutOfBoundsBorderFactory<>()
                    );
*/
                    result = ij.op().filter().convolve(
                            (RandomAccessibleInterval<UnsignedByteType>) input.getImgPlus().getImg(),
                            ij.op().create().kernel(edgeKernel(), new DoubleType()),
                            new OutOfBoundsBorderFactory<>()
                    );

                    ij.op().convert().imageType(
                            new IterableRandomAccessibleInterval<UnsignedByteType>(output),
                            new IterableRandomAccessibleInterval<>(result),
                            new ClipRealTypes<>()
                    );
                } else if (op.equals("boxblur_convolution")) {
                    convolve(ij, output, (RandomAccessibleInterval) input, boxBlurKernel(Integer.parseInt(System.getenv("B_KERNEL_SIZE"))));
                } else {
                    System.err.println("Unknown op: " + op);
                    System.exit(1);
                }
                Measure.nextRound();
            }

            if (MPIUtils.isRoot()) {
                SCIFIOConfig config = new SCIFIOConfig();
                config.writerSetSequential(true);
                config.writerSetCompression("Uncompressed");
                final RandomAccessibleInterval finalOutput = output;
                measure("write", () -> {
                    ij.scifio().datasetIO().save(ij.dataset().create(finalOutput), outputPath, config);
                });
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    private static SCIFIOConfig createSCIFIOConfig() {
        SCIFIOConfig config = new SCIFIOConfig();
        config.imgOpenerSetIndex(0);
        config.imgOpenerSetComputeMinMax(false);
        String inputType = System.getenv("B_IMG_MODE");
        switch (inputType) {
            case "CELL":
                config.imgOpenerSetImgModes(SCIFIOConfig.ImgMode.CELL);
                break;
            case "ARRAY":
                config.imgOpenerSetImgModes(SCIFIOConfig.ImgMode.ARRAY);
                break;
            case "PLANAR":
                config.imgOpenerSetImgModes(SCIFIOConfig.ImgMode.PLANAR);
                break;
            default:
                throw new RuntimeException("Unknown B_IMG_MODE: " + inputType);
        }
        return config;
    }

    private static <O extends RealType<O>> void convolve(ImageJ ij, RandomAccessibleInterval<O> output, RandomAccessibleInterval<O> input, double[][] kernel) {
        ij.op().filter().convolve(
                output,
                Views.extendMirrorSingle(input),
                ij.op().create().kernel(kernel, new DoubleType())
        );
    }

}
