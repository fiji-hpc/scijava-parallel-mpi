package cz.it4i.scijava.mpi.gatherer;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.IterableRandomAccessibleInterval;

public abstract class AbstractGatherer<O> implements Gatherer<O> {
    protected Object unwrap(IterableInterval<O> iterableInterval) {
        Object source = iterableInterval;

        if(source instanceof IterableRandomAccessibleInterval) {
            source = ((IterableRandomAccessibleInterval) source).getSource();
        }

        if(source instanceof IntervalView) {
            source = ((IntervalView<O>) source).getSource();
        }

        if(source instanceof Dataset) {
            source = ((Dataset) source).getImgPlus();
        }

        if(source instanceof ImgPlus) {
            source = ((ImgPlus<O>) source).getImg();
        }

        return source;
    }
}
