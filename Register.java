
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

/**
 * Represents a register of a processor that holds a value. We use an
 * interface for class instead of a primitive type for clarity and to
 * allow a subclass {@link ObservableRegister} that is {@link
 * Observable}.
 */
public interface Register {

    /**
     * Stores a value in the register after truncating it to the bit
     * size of this register.
     *
     * @param value Value to be stored in the register.
     */
    void set(final int value);

    /**
     * Returns the value stored in the register.
     *
     * @return Value stored in register.
     */
    int get();
}
