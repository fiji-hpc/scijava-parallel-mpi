#include <mpi.h>
#include "utils.h"

int world_size() {
    int world_size;
    MPI_CHECK(MPI_Comm_size(MPI_COMM_WORLD, &world_size));
    return world_size;
}

int world_rank() {
    int rank;
    MPI_CHECK(MPI_Comm_rank(MPI_COMM_WORLD, &rank));
    return rank;
}

uint64_t numElements(int64_t *dims, int n_dims) {
    uint64_t total = 1;
    for(int i = 0; i < n_dims; i++) {
        total *= dims[i];
    }
    return total;
}
