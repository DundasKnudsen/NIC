
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

/**
 * Container class that indicates if a computer is running on its own
 * or interactively.
 *
 * @author Douglas Wikstrom
 */
public class GenericRunning implements Running {

    /**
     * Running status of computer.
     */
    protected boolean status;

    /**
     * Creates an instance with the given state.
     *
     * @param status Initial running status of computer.
     */
    public GenericRunning(final boolean status) {
        this.status = status;
    }

    /**
     * Sets the status.
     *
     * @param status Running status of computer.
     */
    public void set(final boolean status) {
        this.status = status;
    }

    /**
     * Gets the status.
     *
     * @return Running status of computer.
     */
    public boolean get() {
        return status;
    }
}
