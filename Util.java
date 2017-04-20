
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Utility functions.
 */
public class Util {

    /**
     * Reads the contents of a file and returs it as a string.
     *
     * @param file File containing string.
     */
    public static String readString(final File file) throws NICException {
        final String fname = file.getName();
        BufferedReader in = null;
        try {

            // This looks ugly, but gives simpler robust closing of
            // the readers and the stream in the finally clause below.
            in = new BufferedReader(
                     new InputStreamReader(
                         new FileInputStream(file), "UTF-8"));

            final String prog = in.readLine();
            if (prog == null) {
                throw new NICException("Cannot read " + fname + "!");
            }
            return prog;

        } catch (final FileNotFoundException fnfe) {
            throw new NICException("Cannot find " + fname + "!");
        } catch (final IOException ioe) {
            throw new NICException("Cannot open " + fname + "!");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                throw new Error("Unable to close file! ("
                                + file.toString() + ")");
            }
        }
    }
}
