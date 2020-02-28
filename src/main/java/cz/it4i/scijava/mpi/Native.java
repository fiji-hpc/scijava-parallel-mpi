package cz.it4i.scijava.mpi;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class Native {
    public static void main(String[] args) throws Exception {
        ImageJ ij = new ImageJ();

        Dataset img = ij.scifio().datasetIO().open("../run/datasets/nums-1-5.tif");
        RandomAccessibleInterval<UnsignedByteType> outputImg = ij.op().create().img(img.getImgPlus(), new UnsignedByteType());
        ij.op().filter().min(
                (IterableInterval) outputImg,
                (RandomAccessibleInterval) img,
                new RectangleShape(1, false)
        );

        if(MPIUtils.isRoot()) {
            ij.ui().show(outputImg);
            ij.scifio().datasetIO().save(ij.dataset().create(outputImg), "/tmp/test.tif");
        }
    }

    public static void copyToNative(PlanarImg img, Memory memory) {
        long offset = 0;
        for(int i = 0; i < img.numSlices(); i++) {
            ByteArray arr = (ByteArray) img.getPlane(i);

            memory.write(offset, arr.getCurrentStorageArray(), 0, arr.getArrayLength());
            offset += arr.getArrayLength();
        }
    }

    public static void copyFromNative(PlanarImg img, Memory memory) {
        long offset = 0;
        for(int i = 0; i < img.numSlices(); i++) {
            ByteArray arr = (ByteArray) img.getPlane(i);

            memory.read(offset, arr.getCurrentStorageArray(), 0, arr.getArrayLength());
            offset += arr.getArrayLength();
        }
    }

    public interface NativeLib extends Library {
       NativeLib INSTANCE = com.sun.jna.Native.load("scijava_parallel_mpi_native", NativeLib.class);
        void minfilter(Memory output, Memory input, long[] dims, int n_dims, int neighSize);
        void mpirank(Memory output, Memory input, long[] dims, int n_dims);
        void convolve(Memory output, Memory input, long[] dims, int n_dims, Memory kernel, long[] kernel_dims, int n_kernel);
        void add(Memory output, Memory input, long elements, byte scalar);
    }
}
