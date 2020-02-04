package com.mycompany.imagej.gatherer;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.chunk.Chunk;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class GenericGatherer<O> implements Gatherer<O> {
    @Override
    public boolean gather(Chunk<O> chunks) {
        int root = 0;
        for (Chunk<O> chunk : chunks) {
            byte[] storage = new byte[(int) chunk.getLen()];

            if (root == MPIUtils.getRank()) {
                Cursor<O> cursor = chunk.cursor();
                int i = 0;
                while (cursor.hasNext()) {
                    cursor.fwd();
                    storage[i++] = ((UnsignedByteType) cursor.get()).getByte();
                }
            }

            MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(storage, storage.length, MPIUtils.MPI_BYTE, root++, MPIUtils.MPI_COMM_WORLD);

            if (root != MPIUtils.getRank()) {
                Cursor<O> cursor = chunk.cursor();
                int i = 0;
                while (cursor.hasNext()) {
                    cursor.fwd();
                    ((UnsignedByteType) cursor.get()).setByte(storage[i++]);
                }
            }
        }

        return true;
    }
}
