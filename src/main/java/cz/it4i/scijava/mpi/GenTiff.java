package cz.it4i.scijava.mpi;

import net.imagej.ImageJ;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import java.io.FileInputStream;
import java.io.InputStream;

public class GenTiff {
    public static void main(String... args) throws Exception {
        ImageJ ij = new ImageJ();

//        Object o = ij.scifio().datasetIO().open("test_10x10x10x10x10.tiff");

        long[] dims = new long[args.length];
        int i = 0;
        StringBuilder name = new StringBuilder();
        name.append("test_");
        for(String arg: args) {
            if(i != 0) {
                name.append("x");
            }
            name.append(arg);
            dims[i++] = Integer.parseInt(arg);
        }
        name.append(".ome.tif");

        PlanarImg<UnsignedByteType, ByteArray> img = (PlanarImg<UnsignedByteType, ByteArray>) new PlanarImgFactory<>(new UnsignedByteType()).create(dims);
        InputStream rand = new FileInputStream("/dev/urandom");
        for(int p = 0; p < img.numSlices(); p++) {
            ByteArray data = img.getPlane(p);
            if(rand.read(data.getCurrentStorageArray(), 0, data.getArrayLength()) != data.getArrayLength()) {
                throw new RuntimeException("failed to read");
            }
            System.out.print("\r" + p + "/" + img.numSlices());
        }
        System.out.println("\nsaving...");

        ij.scifio().datasetIO().save(ij.dataset().create(img), name.toString());
        System.exit(0);
    }
}
