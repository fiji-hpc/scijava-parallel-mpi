package cz.it4i.scijava.mpi.gatherer;

import cz.it4i.scijava.mpi.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;

public class RandomAccessibleIntervalGatherer {
   public static <O> void gather(Chunk<O> chunks) {
      List<Gatherer<O>> gatherers = new ArrayList<>();
      gatherers.add(new PlanarGatherer<>());
      gatherers.add(new ImgGatherer<>());
//      gatherers.add(new CellGatherer<>());
      gatherers.add(new GenericGatherer<>());

      boolean gathered = false;
      for (Gatherer<O> gatherer : gatherers) {
         if (gatherer.gather(chunks)) {
            //System.out.println("Gathered with: " + gatherer.getClass().getName());
            gathered = true;
            break;
         }
      }

      if (!gathered) {
         throw new RuntimeException("Could not gather");
      }
   }
}
