package cz.it4i.scijava.mpi;

import net.imagej.ops.DefaultOpMatchingService;
import net.imagej.ops.OpCandidate;
import net.imagej.ops.OpEnvironment;
import net.imagej.ops.OpRef;
import org.scijava.module.ModuleInfo;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

import java.util.ArrayList;
import java.util.List;

@Plugin(type = Service.class, priority = 10000)
public class MPIOpMatchingService extends DefaultOpMatchingService {
    @Override
    public OpCandidate findMatch(OpEnvironment ops, List<OpRef> refs) {
        OpCandidate candidate = super.findMatch(ops, refs);
        this.log().debug("OP: " + candidate.getModule());
        return candidate;
    }

    @Override
    public List<OpCandidate> findCandidates(OpEnvironment ops, List<OpRef> refs) {
        boolean enableMPIOps = System.getenv("B_NO_MPI_OPS") == null;

        final List<OpCandidate> matches = new ArrayList<>();
        for (final OpCandidate candidate : super.findCandidates(ops, refs)) {
            final ModuleInfo info = candidate.cInfo();

            if(info.is("MPI")) {
                if(!enableMPIOps) {
                    continue;
                }
            }
            matches.add(candidate);
        }

        return matches;
    }
}
