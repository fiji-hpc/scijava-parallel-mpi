package com.mycompany.imagej;

import io.scif.Metadata;
import io.scif.config.SCIFIOConfig;
import io.scif.services.DefaultDatasetIOService;
import net.imagej.Dataset;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;
import static com.mycompany.imagej.Measure.measureCatch;

import java.io.IOException;
import java.util.List;

@Plugin(type = Service.class)
public class MPIDatasetIOService extends DefaultDatasetIOService {
    @Override
    public Dataset open(String source, SCIFIOConfig config) throws IOException {
        return measureCatch("read", () -> super.open(source, config));
    }

    @Override
    public List<Dataset> openAll(String source, SCIFIOConfig config) throws IOException {
        return measureCatch("read", () -> super.openAll(source, config));
    }

    @Override
    public Metadata save(Dataset dataset, String destination, SCIFIOConfig config) throws IOException {
        if(MPIUtils.isRoot()) {
            config.writerSetSequential(true);
            config.writerSetCompression("Uncompressed");

            return measureCatch("write", () -> super.save(dataset, destination, config));
        }
        return null;
    }
}
