#include <mpi.h>
#include "split.h"
#include "utils.h"

static uint32_t type_multiplier(uint64_t elements) {
  uint32_t multiplier = 1;
  while(elements >= 0x7fffffff) {
    elements /= 2;
    multiplier *= 2;
  }
  return multiplier;
}

void split_init(struct split *split, uint8_t *output, uint64_t elements) {
    split->begin = output;
    split->elements = elements;
    split->type_multiplier = type_multiplier(elements);

    int rankSize = world_size();
    split->per_node = (elements + rankSize) / rankSize;
    uint64_t rem = split->per_node % split->type_multiplier;
    if(rem != 0) {
      split->per_node = split->per_node + split->type_multiplier - rem;
    }

    split->node_sizes = (uint64_t*) malloc(sizeof(*split->node_sizes) * rankSize);
    split->node_offsets = (uint64_t*) malloc(sizeof(*split->node_offsets) * rankSize);
    for(int i = 0; i < rankSize; i++) {
        split->node_sizes[i] = split->per_node;
        split->node_offsets[i] = i * split->per_node;
    }
    split->node_sizes[rankSize - 1] = MIN(
        split->node_offsets[rankSize - 1] + split->per_node,
        elements
    ) - split->node_offsets[rankSize - 1];
}

uint8_t* split_myblock(struct split *split, uint64_t *len) {
    int rank = world_rank();
    *len = split->node_sizes[rank];
    return split->begin + split->node_offsets[rank];
}

void split_myblock_pos(struct split *split, uint64_t *begin, uint64_t *end) {
    int rank = world_rank();
    *begin = split->node_offsets[rank];
    *end = *begin + split->node_sizes[rank];
}

void split_sync(struct split *split) {
    MPI_Datatype type = MPI_BYTE;
    if(split->type_multiplier != 1) {
      MPI_CHECK(MPI_Type_contiguous(split->type_multiplier, MPI_BYTE, &type));
      MPI_CHECK(MPI_Type_commit(&type));
    }

    int total_nodes = world_size();
    int recvcounts[total_nodes];
    int displs[total_nodes];
    for(int i = 0; i < total_nodes; i++) {
      recvcounts[i] = split->node_sizes[i] / split->type_multiplier;
      displs[i] = split->node_offsets[i] / split->type_multiplier;
    }

    int rank = world_rank();
    bench_start();
    MPI_CHECK(MPI_Allgatherv(
        split->begin + split->node_offsets[rank],
        recvcounts[rank],
        type,
        split->begin,
        recvcounts,
        displs,
        type,
        MPI_COMM_WORLD
    ));
    bench_stop("MPI_Allgatherv");

    if(split->type_multiplier != 1) {
      MPI_CHECK(MPI_Type_free(&type));
    }
}

void split_free(struct split *split) {
    free(split->node_sizes);
    free(split->node_offsets);
}
