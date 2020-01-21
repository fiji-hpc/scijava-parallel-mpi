package com.mycompany.imagej;

import io.scif.config.SCIFIOConfig;
import io.scif.img.IO;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.scijava.io.location.FileLocation;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;


public class Main {
    static double[][] identity(int size) {
        double [][] kernel = new double[size][size];
        for(int r = 0; r < size; r++) {
            for(int c = 0; c < size; c++) {
                kernel[r][c] = 0;
            }
        }
        kernel[size / 2][size / 2] = 1;
        return kernel;
    }

    public static void main(String[] args) throws Exception{
        ImageJ ij = new ImageJ();

        SCIFIOConfig conf = new SCIFIOConfig();
        conf.imgOpenerSetComputeMinMax(false);
        conf.imgOpenerSetImgModes(SCIFIOConfig.ImgMode.CELL);
        //Img<UnsignedByteType> img = IO.openImgs(args[0], new UnsignedByteType(), conf).get(0);
        Img<UnsignedByteType> img = IO.open(new FileLocation(args[0]), new UnsignedByteType(), conf).get(0);
        System.out.println(img);

        convolve(ij, img);

        System.exit(0);
    }

    private static void convolve(ImageJ ij, RandomAccessibleInterval<UnsignedByteType> img) throws Exception {
        int kernel_size = 3;
        RandomAccessibleInterval<DoubleType> kernel = ij.op().create().kernel(identity(kernel_size), new DoubleType());

        RandomAccessibleInterval<UnsignedByteType> result = ij.op().create().img(img);
        ij.op().filter().convolve(result, Views.extendMirrorSingle(img), kernel);
        if(MPIUtils.isRoot()) {
            saveImage("result/result.pgm", result);
        }
        saveImage("result/result" + MPIUtils.getRank() + ".pgm", result);
    }

    private static void saveImage(String path, RandomAccessibleInterval img) throws Exception {
        // ij.io().save(ij.dataset().create(result), "result.tif");

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(path), 1024 * 1024 * 512);
        String header = "P5\n" + img.dimension(0) + " " + img.dimension(1) + "\n255\n";
        out.write(header.getBytes());
        for (Object o : new IterableRandomAccessibleInterval<>(img)) {
            UnsignedByteType b = (UnsignedByteType) o;
            out.write((byte) b.get());
        }
        out.close();
    }
}
