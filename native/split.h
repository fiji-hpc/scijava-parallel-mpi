#include <stdint.h>

struct split {
    uint8_t* begin;
    uint64_t elements;

    uint32_t type_multiplier;

    uint64_t per_node;
    uint64_t *node_offsets;
    uint64_t *node_sizes;
};

void split_init(struct split *split, uint8_t *output, uint64_t elements);
uint8_t* split_myblock(struct split *split, uint64_t *len);
void split_myblock_pos(struct split *split, uint64_t *begin, uint64_t *end);
void split_sync(struct split *split);
void split_free(struct split *split);
