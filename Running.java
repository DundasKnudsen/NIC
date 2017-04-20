
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

/**
 * Indicates if a computer is running on its own or not.
 *
 * @author Douglas Wikstrom
 */
public interface Running {

    /**
     * Sets the status.
     *
     * @param status Running status of computer.
     */
    public void set(final boolean status);

    /**
     * Gets the status.
     *
     * @return Status.
     */
    public boolean get();
}
