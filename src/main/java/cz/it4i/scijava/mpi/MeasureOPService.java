package cz.it4i.scijava.mpi;

import net.imagej.ops.DefaultOpService;
import net.imagej.ops.Op;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

import static cz.it4i.scijava.mpi.Measure.measureCatch;

@Plugin(type = Service.class)
public class MeasureOPService extends DefaultOpService {
    @Override
    public Object run(Op op, Object... args) {
        return measureCatch(op.toString(), () -> super.run(op, args));
    }

    @Override
    public Object run(String name, Object... args) {
        return measureCatch(name, () -> super.run(name, args));
    }

    @Override
    public Object run(Class<? extends Op> type, Object... args) {
        return measureCatch(type.getCanonicalName(), () -> super.run(type, args));
    }
}
