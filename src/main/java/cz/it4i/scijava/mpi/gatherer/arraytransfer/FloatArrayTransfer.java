package cz.it4i.scijava.mpi.gatherer.arraytransfer;

import cz.it4i.scijava.mpi.MPIUtils;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.real.FloatType;

public class FloatArrayTransfer extends ArrayTransfer<FloatType> {
    private float[] array;

    @Override
    public void allocateArray(int len) {
        this.array = new float[len];
    }

    @Override
    public void write(Cursor<FloatType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            array[i++] = cursor.get().getRealFloat();
        }
    }

    @Override
    public void transfer(int root) {
        MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(array, limit(), MPIUtils.MPI_FLOAT, root, MPIUtils.MPI_COMM_WORLD);
    }

    @Override
    public void read(Cursor<FloatType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            cursor.get().setReal(array[i++]);
        }
    }
}
