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

void index_to_pos(uint64_t index, int64_t *position, int64_t *dims, int n) {
  for(int d = 0; d < n - 1; d++) {
    uint64_t pos = index / dims[d];
    position[d] = index - pos * dims[d];
    index = pos;
  }
  position[n - 1] = index;
}

void fwd(int64_t *position, int64_t *dims, int n_dims) {
  for(int d = 0; d < n_dims; d++) {
    position[d]++;
    if(position[d] >= dims[d]) {
      position[d] = 0;
    } else {
      break;
    }
  }
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
