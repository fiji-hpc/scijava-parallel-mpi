package com.mycompany.imagej;

import io.scif.img.IO;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import java.io.IOException;


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

        Img<UnsignedByteType> img = IO.openImgs("./lena.png", new UnsignedByteType()).get(0);

        convolve(ij, img);

        System.exit(0);
    }

    private static void convolve(ImageJ ij, RandomAccessibleInterval<UnsignedByteType> img) throws IOException {
        int kernel_size = 3;
        RandomAccessibleInterval<DoubleType> kernel = ij.op().create().kernel(identity(kernel_size), new DoubleType());

        RandomAccessibleInterval<UnsignedByteType> result = ij.op().create().img(img);
        ij.op().filter().convolve(result, Views.extendMirrorSingle(img), kernel);
        if(MPIUtils.isRoot()) {
            ij.io().save(ij.dataset().create(result), "result.png");
        }
    }

}
