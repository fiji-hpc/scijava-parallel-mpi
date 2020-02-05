package com.mycompany.imagej.gatherer.arraytransfer;

import com.mycompany.imagej.MPIUtils;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.integer.LongType;

public class LongArrayTransfer extends ArrayTransfer<LongType> {
    private long[] array;

    @Override
    public void allocateArray(int len) {
        this.array = new long[len];
    }

    @Override
    public void write(Cursor<LongType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            array[i++] = cursor.get().getIntegerLong();
        }
    }

    @Override
    public void transfer(int root) {
        MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(array, limit(), MPIUtils.MPI_UNSIGNED_LONG, root, MPIUtils.MPI_COMM_WORLD);
    }

    @Override
    public void read(Cursor<LongType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            cursor.get().setLong(array[i++]);
        }
    }
}
