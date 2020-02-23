#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#define MPI_CHECK(expr) if((expr) != 0) {fprintf(stderr, "MPI failed: %s:%d\n", __FILE__, __LINE__); exit(1);}
#define MIN(a, b) ((a) < (b) ? (a) : (b))

int world_size();
int world_rank();
uint64_t numElements(int64_t *dims, int n_dims);
void bench_start();
void bench_stop(const char *desc);
