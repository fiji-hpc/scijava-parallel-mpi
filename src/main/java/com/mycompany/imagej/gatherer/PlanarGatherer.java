package com.mycompany.imagej.gatherer;

import com.mycompany.imagej.NonBlockingBroadcast;
import com.mycompany.imagej.chunk.Chunk;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.planar.PlanarImg;

import java.util.ArrayList;
import java.util.List;

public class PlanarGatherer<O> extends AbstractGatherer<O> {
    @Override
    public boolean gather(Chunk<O> chunks) {
        Object source = this.unwrap(chunks.getData());

        if(!(source instanceof PlanarImg)) {
            return false;
        }

        PlanarImg img = (PlanarImg) source;
        NonBlockingBroadcast transfer = new NonBlockingBroadcast();
        int currentRoot = 0;

        ArrayDataAccess<?> c = img.getPlane(0);
        int planeOffset = 0;
        int lastPlane = 0;

        for(Chunk<O> chunk: chunks.allChunks()) {
            List<NonBlockingBroadcast.Block> transferBlocks = new ArrayList<>();

            long sent = 0;
            long totalLen = chunk.getLen();
            while(sent < totalLen) {
                int length = (int) Math.min(totalLen - sent, c.getArrayLength() - planeOffset);
                transferBlocks.add(new NonBlockingBroadcast.Block(c.getCurrentStorageArray(), planeOffset, length));

                planeOffset += length;
                if(planeOffset > c.getArrayLength()) {
                    throw new RuntimeException("UNEXPECTED");
                }
                if(planeOffset == c.getArrayLength()) {
                    lastPlane++;
                    planeOffset = 0;

                    if(lastPlane < img.numSlices()) {
                        c = img.getPlane(lastPlane);
                    }
                }

                sent += length;
            }

            transfer.requestTransfer(currentRoot++, transferBlocks);
        }
        if(lastPlane != img.numSlices()) {
            throw new RuntimeException("Unexpected");
        }

        transfer.waitForTransfer();

        return true;
    }
}
