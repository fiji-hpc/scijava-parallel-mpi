package cz.it4i.scijava.mpi.gatherer;

import cz.it4i.scijava.mpi.chunk.Chunk;

public interface Gatherer<O> {
    boolean gather(Chunk<O> chunks);
}
