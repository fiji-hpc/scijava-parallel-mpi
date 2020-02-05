package com.mycompany.imagej;

import com.mycompany.imagej.ops.MPIRankColor;
import io.scif.config.SCIFIOConfig;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IterableRandomAccessibleInterval;

import static com.mycompany.imagej.Measure.measure;
import static com.mycompany.imagej.Measure.measureCatch;


public class Main {
    static double[][] boxBlurKernel(int size) {
        double [][] kernel = new double[size][size];
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                kernel[r][c] = 1.0 / (size * size);
            }
        }
        return kernel;
    }

    static double[][] identityKernel(int size) {
        double [][] kernel = new double[size][size];
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

    public static void main(String[] args) {
        try {
            measure("total", () -> run(args));
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    private static void run(String[] args) throws Exception {
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

        RandomAccessibleInterval<? extends NativeType<?>> output = ij.op().create().img(input);

        int rounds = 1;
        if(System.getenv("B_ROUNDS") != null) {
            rounds = Integer.parseInt(System.getenv("B_ROUNDS"));
        }
        for(int i = 0; i < rounds; i++) {
            Utils.rootPrint("### Round " + i);
            MPIUtils.barrier();

            if (op.equals("rank_color")) {
                ij.op().run(MPIRankColor.class, output, input);
            } else if (op.equals("convolution")) {
                convolution(ij, (RandomAccessibleInterval) input, (RandomAccessibleInterval) output);
            } else if(op.equals("project")) {
                long[] dims = new long[input.numDimensions() - 1];
                for(int j = 0; j < input.numDimensions() - 1; j++) {
                    dims[j] = input.dimension(j);
                }

                output = ij.op().create().img(new FinalDimensions(dims), (NativeType) input.getType());

                UnaryComputerOp mean_op = (UnaryComputerOp) ij.op().op(Ops.Stats.Max.NAME,
                            input.getImgPlus());

                ij.op().transform().project(
                        new IterableRandomAccessibleInterval<>(output),
                        input,
                        mean_op,
                        input.numDimensions() - 1
                );
            } else {
                System.err.println("Unknown op: " + op);
                System.exit(1);
            }

            MPIUtils.barrier();
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
        MPIUtils.barrier();
    }

    private static <I extends RealType<I>, O extends RealType<O>> void convolution(ImageJ ij, RandomAccessibleInterval<I> input, RandomAccessibleInterval<O> output) {
        measureCatch("total_convolution", () -> {
            ij.op().filter().convolve(
                    output,
                    input,
                    ij.op().create().kernel(getKernel(), new DoubleType())
            );
        });
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

    private static double[][] getKernel() {
        String kernel = System.getenv("B_KERNEL");
        int size = Integer.parseInt(System.getenv("B_KERNEL_SIZE"));
        switch (kernel) {
            case "identity":
                return identityKernel(size);
            case "boxblur":
                return boxBlurKernel(size);
            case "edge":
                return edgeKernel();
            default:
                throw new RuntimeException("Unknown B_KERNEL: " + kernel);
        }
    }
}
