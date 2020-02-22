package cz.it4i.scijava.mpi.gatherer.arraytransfer;

import cz.it4i.scijava.mpi.MPIUtils;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.integer.GenericIntType;

public class IntArrayTransfer extends ArrayTransfer<GenericIntType> {
    private int[] array;

    @Override
    public void allocateArray(int len) {
        this.array = new int[len];
    }

    @Override
    public void write(Cursor<GenericIntType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            array[i++] = cursor.get().getInteger();
        }
    }

    @Override
    public void transfer(int root) {
        MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(array, limit(), MPIUtils.MPI_UNSIGNED, root, MPIUtils.MPI_COMM_WORLD);
    }

    @Override
    public void read(Cursor<GenericIntType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            cursor.get().setInteger(array[i++]);
        }
    }
}
