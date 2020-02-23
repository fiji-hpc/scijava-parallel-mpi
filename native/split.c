#include <mpi.h>
#include "split.h"
#include "utils.h"

void split_init(struct split *split, uint8_t *output, uint64_t elements) {
    split->begin = output;
    split->elements = elements;

    int rankSize = world_size();

    uint64_t perNode = (elements + rankSize) / rankSize;

    split->recvcounts = (int*) malloc(sizeof(int) * rankSize);
    split->displs = (int*) malloc(sizeof(int) * rankSize);
    for(int i = 0; i < rankSize; i++) {
        split->recvcounts[i] = perNode;
        split->displs[i] = i * perNode;
    }
    split->recvcounts[rankSize - 1] = MIN(
        split->displs[rankSize - 1] + perNode,
        elements
    ) - split->displs[rankSize - 1];
}

uint8_t* split_myblock(struct split *split, uint64_t *len) {
    int rank = world_rank();
    *len = split->recvcounts[rank];
    return split->begin + split->displs[rank];
}

void split_myblock_pos(struct split *split, uint64_t *begin, uint64_t *end) {
    int rank = world_rank();
    *begin = split->displs[rank];
    *end = *begin + split->recvcounts[rank];
}

void split_sync(struct split *split) {
    int rank = world_rank();
    int myOffset = split->displs[rank];
    MPI_CHECK(MPI_Allgatherv(
        split->begin + myOffset,
        split->recvcounts[rank],
        MPI_BYTE,
        split->begin,
        split->recvcounts,
        split->displs,
        MPI_BYTE,
        MPI_COMM_WORLD
    ));
}

void split_free(struct split *split) {
    free(split->recvcounts);
    free(split->displs);
}
