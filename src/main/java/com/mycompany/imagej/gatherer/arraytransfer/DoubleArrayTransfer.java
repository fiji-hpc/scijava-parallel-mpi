package com.mycompany.imagej.gatherer.arraytransfer;

import com.mycompany.imagej.MPIUtils;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.real.DoubleType;

public class DoubleArrayTransfer extends ArrayTransfer<DoubleType> {
    private double[] array;

    @Override
    public void allocateArray(int len) {
        this.array = new double[len];
    }

    @Override
    public void write(Cursor<DoubleType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            array[i++] = cursor.get().getRealDouble();
        }
    }

    @Override
    public void transfer(int root) {
        MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(array, limit(), MPIUtils.MPI_DOUBLE, root, MPIUtils.MPI_COMM_WORLD);
    }

    @Override
    public void read(Cursor<DoubleType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            cursor.get().setReal(array[i++]);
        }
    }
}
