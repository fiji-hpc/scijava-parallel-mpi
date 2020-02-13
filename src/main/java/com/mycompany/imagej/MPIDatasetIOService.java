package com.mycompany.imagej;

import io.scif.Metadata;
import io.scif.config.SCIFIOConfig;
import io.scif.services.DefaultDatasetIOService;
import net.imagej.Dataset;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

import java.io.IOException;

@Plugin(type = Service.class)
public class MPIDatasetIOService extends DefaultDatasetIOService {
    @Override
    public Metadata save(Dataset dataset, String destination, SCIFIOConfig config) throws IOException {
        if(MPIUtils.isRoot()) {
            return super.save(dataset, destination, config);
        }
        return null;
    }
}
