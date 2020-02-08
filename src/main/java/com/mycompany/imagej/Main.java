package com.mycompany.imagej;

import com.mycompany.imagej.ops.MPIRankColor;
import io.scif.config.SCIFIOConfig;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.FinalDimensions;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;

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

    private static <T extends RealType<T>> void run(String[] args) throws Exception {
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

        RandomAccessibleInterval<? extends NativeType<?>> output = null;

        if (op.equals("rank_color")) {
            output = ij.op().create().img(input);
            ij.op().run(MPIRankColor.class, output, input);
        } else if (op.equals("convolution")) {
            output = ij.op().create().img(input);
            convolution(ij, (RandomAccessibleInterval) input, (RandomAccessibleInterval) output);
        } else if(op.equals("project")) {
            long[] dims = new long[input.numDimensions() - 1];
            for (int j = 0; j < input.numDimensions() - 1; j++) {
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
        } else if(op.equals("minmax")) {
            Pair result = ij.op().stats().minMax((ImgPlus) input.getImgPlus());
            System.out.println("min: " + result.getA());
            System.out.println("max: " + result.getB());
        } else if(op.equals("threshold")) {
            Img<BitType> result = ij.op().create().img(input, new BitType());

            double min = input.firstElement().getMinValue();
            double max = input.firstElement().getMaxValue();
            T threshold = (T) input.firstElement().createVariable();
            threshold.setReal((max - min) / 2.0);

            T maxVal = (T) input.firstElement().createVariable();
            maxVal.setReal(input.firstElement().getMaxValue());

            ij.op().threshold().apply(
                    result,
                    new IterableRandomAccessibleInterval(input),
                    threshold
            );

            output = ij.op().convert().uint8(result);
            output = (RandomAccessibleInterval) ij.op().math().multiply(
                    (IterableInterval<UnsignedByteType>) output,
                    new UnsignedByteType(255)
            );
        } else if(op.equals("add")) {
            T scalar = (T) input.firstElement().createVariable();
            scalar.setReal(2);

            output = ij.op().create().img(input);
            ij.op().math().add(
                    (RandomAccessibleInterval<T>) output,
                    new IterableRandomAccessibleInterval<T>((RandomAccessibleInterval<T>) input),
                    scalar
            );
        } else if(op.equals("gauss")) {
            output = ij.op().create().img(input);
            ij.op().filter().gauss(
                    (RandomAccessibleInterval) output,
                    (RandomAccessible) Views.extendMirrorSingle(input),
                    1.0D,
                    1.0D,
                    1.0D
            );
        } else if(op.equals("filter.max")) {
            int neighSize = Integer.parseInt(System.getenv("B_NEIGH_SIZE"));

            output = ij.op().create().img(input);
            ij.op().filter().max(
                    new IterableRandomAccessibleInterval(output),
                    (RandomAccessibleInterval) input,
                    new RectangleShape(neighSize, false)
            );
        } else {
            System.err.println("Unknown op: " + op);
            System.exit(1);
        }

        if (MPIUtils.isRoot() && output != null) {
            SCIFIOConfig config = new SCIFIOConfig();
            config.writerSetSequential(true);
            config.writerSetCompression("Uncompressed");
            final RandomAccessibleInterval finalOutput = output;
            measure("write", () -> {
                ij.scifio().datasetIO().save(ij.dataset().create(finalOutput), outputPath, config);
            });
        }
    }

    private static <I extends RealType<I>, O extends RealType<O>> void convolution(ImageJ ij, RandomAccessibleInterval<I> input, RandomAccessibleInterval<O> output) {
        measureCatch("total_convolution", () -> {
            ij.op().filter().convolve(
                    output,
                    Views.extendMirrorSingle(input),
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
