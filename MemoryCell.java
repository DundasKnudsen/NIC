
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

/**
 * Memory cell that stores a block. This is meant to simulate a
 * physical block of memory which on a physical computer typically
 * consists of 8 bits, but here we use fewer bits for educational
 * purposes.
 *
 * <p>
 *
 * We use an interface for class instead of a primitive type for
 * clarity and to allow a subclass {@link ObservableRegister} that is
 * {@link Observable}.
 */
public interface MemoryCell {

    /**
     * Returns the block stored in this cell.
     *
     * @return Block stored in this cell.
     */
    int get();

    /**
     * Sets the block stored in this cell. The input is truncated to
     * the right number of bits.
     *
     * @param block Block to be stored in this cell.
     */
    void set(final int block);
}
