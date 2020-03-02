#include <omp.h>
#include <mpi.h>
#include "utils.h"
#include "split.h"


void add(uint8_t *output, uint8_t *input, uint64_t elements, uint8_t scalar) {
  struct split split;
  split_init(&split, output, elements);

  uint64_t node_begin, node_end;
  split_myblock_pos(&split, &node_begin, &node_end);

  bench_start();
  #pragma omp parallel for
  for(uint64_t cur = node_begin; cur < node_end; cur++) {
    output[cur] = input[cur] + scalar;
  }
  bench_stop("add");
  bench_start();
  MPI_CHECK(MPI_Barrier(MPI_COMM_WORLD));
  bench_stop("barier");

  split_sync(&split);
  split_free(&split);
}
