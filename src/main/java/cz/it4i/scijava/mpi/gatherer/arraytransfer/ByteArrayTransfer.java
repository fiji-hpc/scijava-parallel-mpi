package cz.it4i.scijava.mpi.gatherer.arraytransfer;

import cz.it4i.scijava.mpi.MPIUtils;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.integer.GenericByteType;

public class ByteArrayTransfer extends ArrayTransfer<GenericByteType> {
    private byte[] array;

    @Override
    public void allocateArray(int len) {
        this.array = new byte[len];
    }

    @Override
    public void write(Cursor<GenericByteType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            array[i++] = cursor.get().getByte();
        }
    }

    @Override
    public void transfer(int root) {
        MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(array, limit(), MPIUtils.MPI_BYTE, root, MPIUtils.currentComm);
    }

    @Override
    public void read(Cursor<GenericByteType> cursor) {
        int i = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            cursor.get().setByte(array[i++]);
        }
    }
}
