package cz.it4i.scijava.mpi.gatherer;

import cz.it4i.scijava.mpi.NonBlockingBroadcast;
import cz.it4i.scijava.mpi.chunk.Chunk;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;

public class ImgGatherer<O> extends AbstractGatherer<O> {
    @Override
    public boolean gather(Chunk<O> chunks) {
        Object source = this.unwrap(chunks.getData());

        if(!(source instanceof ArrayImg)) {
            return false;
        }

        ArrayImg img = (ArrayImg) source;
        NonBlockingBroadcast transfer = new NonBlockingBroadcast();
        int root = 0;
        for(Chunk<O> chunk: chunks.allChunks()) {
            transfer.requestTransfer(
                    root++,
                    (ArrayDataAccess) img.update(null),
                    (int) chunk.getOffset(),
                    (int) chunk.getLen()
            );
        }

        transfer.waitForTransfer();
        return true;
    }
}
