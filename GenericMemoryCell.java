
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

/**
 * Memory cell without any additional properties that simply stores a
 * block.
 */
public class GenericMemoryCell implements MemoryCell {

    /**
     * Storage of this memory cell.
     */
    protected int block;

    /**
     * Bit-size of this memory cell.
     */
    protected int BLOCKSIZE;

    /**
     * Block where all bits equal one.
     */
    protected int BLOCKMASK;

    /**
     * Creates a memory cell initialized to the zero block.
     *
     * @param BLOCKSIZE Bit-size of this memory cell.
     */
    public GenericMemoryCell(final int BLOCKSIZE) {
        this.block = 0;
        this.BLOCKSIZE = BLOCKSIZE;
        this.BLOCKMASK = (1 << BLOCKSIZE) - 1;
    }

    @Override
    public int get() {
        synchronized (this) {
            return block;
        }
    }

    @Override
    public void set(final int block) {
        synchronized (this) {

            // This sets all bits at positions larger than the block
            // size to zero.
            this.block = block & BLOCKMASK;
        }
    }
}
