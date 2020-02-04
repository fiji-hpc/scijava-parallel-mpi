package com.mycompany.imagej.gatherer;

import com.mycompany.imagej.chunk.Chunk;

public interface Gatherer<O> {
    boolean gather(Chunk<O> chunks);
}
