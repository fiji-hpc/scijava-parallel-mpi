#include "utils.h"
#include "split.h"

void mpirank(uint8_t *output, uint8_t *input, int64_t *dims, int n_dims) {
    struct split split;
    split_init(&split, output, numElements(dims, n_dims));

    uint64_t len;
    uint8_t *cur = split_myblock(&split, &len);
    uint8_t *end = cur + len;

    uint8_t color_offset = 10;
    uint8_t val = color_offset + (255 - color_offset) * world_rank() / world_size();
    while(cur != end) {
        *cur = val;
        cur++;
    }

    split_sync(&split);
    split_free(&split);
}
