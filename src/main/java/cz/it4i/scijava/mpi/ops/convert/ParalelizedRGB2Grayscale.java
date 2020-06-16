package cz.it4i.scijava.mpi.ops.convert;

import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.ops.Ops;
import net.imagej.ops.create.img.CreateImgFromDimsAndType;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.view.Views;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

@Plugin(type = Ops.Transform.Project.class, priority = 0)
public class ParalelizedRGB2Grayscale<T extends RealType<T> & NativeType<T>> extends AbstractUnaryFunctionOp<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> implements RGB2Grayscale {
    @Parameter(required = false)
    double redWeight = 0.2126;

    @Parameter(required = false)
    double greenWeight = 0.7152;

    @Parameter(required = false)
    double blueWeight = 0.0722;

     @Override
    public RandomAccessibleInterval<T> calculate(RandomAccessibleInterval<T> input) {
        long[] outputDims = new long[input.numDimensions() - 1];
        outputDims[0] = input.dimension(0);
        outputDims[1] = input.dimension(1);
        for(int i = 3; i < input.numDimensions(); i++) {
            outputDims[i - 1] = input.dimension(i);
        }

        T type = input.randomAccess().get().createVariable();
        ImgFactory<T> factory = new PlanarImgFactory<T>(type);
        RandomAccessibleInterval<T> output = (RandomAccessibleInterval<T>) ops().run(CreateImgFromDimsAndType.class, new FinalInterval(outputDims), type, factory);

        ops().run(Parallel.class, output, (Consumer<Chunk<T>>) chunk -> {
            long[] outputLocation = new long[input.numDimensions() - 1];
            Cursor<T> c = chunk.cursor();
            c.fwd();
            c.localize(outputLocation);

            Cursor<T> red = Views.hyperSlice(input, 2, 0).cursor();
            red.jumpFwd(IntervalIndexer.positionToIndex(outputLocation, outputDims) + 1);
            Cursor<T> green = Views.hyperSlice(input, 2, 1).cursor();
            green.jumpFwd(IntervalIndexer.positionToIndex(outputLocation, outputDims) + 1);
            Cursor<T> blue = Views.hyperSlice(input, 2, 2).cursor();
            blue.jumpFwd(IntervalIndexer.positionToIndex(outputLocation, outputDims) + 1);

            for(T px: chunk) {
                px.setReal(redWeight * red.get().getRealDouble()
                        + greenWeight * green.get().getRealDouble()
                        + blueWeight * blue.get().getRealDouble());

                red.next();
                green.next();
                blue.next();
            }
        });

        return output;
    }
}
