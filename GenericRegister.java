
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

/**
 * Register without any additional properties that can store a
 * value. This simulates a general purpose register in a physical
 * computer.
 */
public class GenericRegister implements Register {

    /**
     * Mask for this register.
     */
    protected int REGMASK;

    /**
     * Value of this register.
     */
    protected int value;

    /**
     * Creates a register with the given bitsize initialized to zero.
     *
     * @param REGSIZE Bit size of contents of this register.
     */
    public GenericRegister(final int REGSIZE) {
        this.REGMASK = (1 << REGSIZE) - 1;
    }

    @Override
    public synchronized void set(final int value) {
        synchronized (this) {
            this.value = value & REGMASK;
        }
    }

    @Override
    public synchronized int get() {
        synchronized (this) {
            return value;
        }
    }
}
