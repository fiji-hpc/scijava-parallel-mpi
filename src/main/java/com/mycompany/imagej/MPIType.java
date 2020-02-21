package com.mycompany.imagej;

import com.sun.jna.Pointer;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public class MPIType {
    public static Pointer toMPI(Object o) {
        if(o instanceof GenericByteType) {
            return MPIUtils.MPI_BYTE;
        } else if(o instanceof ShortType) {
            return MPIUtils.MPI_UNSIGNED_SHORT;
        } else if(o instanceof GenericIntType) {
            return MPIUtils.MPI_UNSIGNED;
        } else if(o instanceof GenericLongType) {
            return MPIUtils.MPI_UNSIGNED_LONG;
        } else if(o instanceof DoubleType) {
            return MPIUtils.MPI_DOUBLE;
        } else if(o instanceof FloatType) {
            return MPIUtils.MPI_FLOAT;
        }

        throw new RuntimeException("Unsupported type: " + o.getClass().getName());
    }

    public static int size(Pointer p) {
        if (p == MPIUtils.MPI_BYTE) {
            return 1;
        } else if (p == MPIUtils.MPI_UNSIGNED_SHORT) {
            return 2;
        } else if (p == MPIUtils.MPI_UNSIGNED) {
            return 4;
        } else if (p == MPIUtils.MPI_UNSIGNED_LONG) {
            return 8;
        } else if (p == MPIUtils.MPI_FLOAT) {
            return 4;
        } else if (p == MPIUtils.MPI_DOUBLE) {
            return 8;
        }
        throw new RuntimeException("Unsupported type");
    }
}
