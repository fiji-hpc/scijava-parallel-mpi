package cz.it4i.scijava.mpi.gatherer.arraytransfer;

import cz.it4i.scijava.mpi.MPIUtils;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.integer.GenericShortType;

public class ShortArrayTransfer extends ArrayTransfer<GenericShortType> {
    private short[] array;

    @Override
    public void allocateArray(int len) {
        this.array = new short[len];
    }

    @Override
    public void write(Cursor<GenericShortType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            array[i++] = cursor.get().getShort();
        }
    }

    @Override
    public void transfer(int root) {
        MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(array, limit(), MPIUtils.MPI_UNSIGNED_SHORT, root, MPIUtils.currentComm);
    }

    @Override
    public void read(Cursor<GenericShortType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            cursor.get().setShort(array[i++]);
        }
    }
}
