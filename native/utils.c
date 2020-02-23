#include <mpi.h>
#include <time.h>
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

static struct timespec start;
void bench_start() {
    clock_gettime(CLOCK_MONOTONIC, &start);
}

void bench_stop(const char *desc) {
  struct timespec end;
  clock_gettime(CLOCK_MONOTONIC, &end);

  uint64_t elapsed = (end.tv_sec * 1000000000UL + end.tv_nsec)
      - (start.tv_sec * 1000000000UL + start.tv_nsec);
  printf("%s: %f\n", desc, (double) (elapsed / 1000000000.0L));
}
