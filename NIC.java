
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

import java.awt.Dimension;
import java.awt.Rectangle;

import se.kth.csc.nic.gui.ComputerFrame;
import se.kth.csc.nic.observable.ObservableComputer;

/**
 * Command line interface for NIC.
 *
 * @author Douglas Wikstrom
 */
public class NIC {

    /**
     * Version of this software derived from the manifest file of the
     * jar-file.
     */
    public static String VERSION =
        NIC.class.getPackage().getSpecificationVersion();

    /**
     * Display name of this software.
     */
    public final static String FULLNAME =
        "Nilsson Instructional Computer (NIC)";

    /**
     * About information used in the user interface.
     */
    public final static String ABOUT =
        FULLNAME
        + "\nVersion " + NIC.VERSION
        + "\nCopyright"
        + "\n1997-2015 Stefan Nilsson <snilsson@nada.kth.se>"
        + "\n2015-2017 Douglas Wikstrom <dog@kth.se>";

    /**
     * Print the message and exit with the exit code.
     *
     * @param message What to print.
     * @param exitCode Exit code of the program.
     */
    protected static void printExit(final String message, final int exitCode) {
        System.out.println(message);
        System.exit(0);
    }

    /**
     * Prints usage information for the command and exits.
     */
    protected static void usage(final String commandName) {
        printExit("Usage: " + commandName + "[-v|-h]", 0);
    }

    /**
     * Prints the version and exits.
     */
    protected static void version() {
        printExit("Version: " + VERSION, 0);
    }

    /**
     * Prints an error message and exits with exit code 1.
     *
     * @param message What to print.
     */
    protected static void errorExit(final String message) {
        printExit("ERROR: " + message, 1);
    }

    /**
     * Command line interface.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {

        final String commandName = "nic";

        if (args.length == 1) {
            if (args[0].equals("-v")) {
                version();
            } else if (args[0].equals("-h")) {
                usage(commandName);
            } else {
                errorExit("Unknown parameter!");
            }
        } else if (args.length > 1) {
            errorExit("Too many parameters!");
        }

        final ObservableComputer oc = new ObservableComputer(256, 4, 2, 16);
        final ComputerFrame cf = new ComputerFrame(FULLNAME, oc);
        cf.pack();

        final Rectangle screenSize =
            cf.getGraphicsConfiguration().getBounds();
        final Dimension size = cf.getPreferredSize();
        cf.setLocation((int) (screenSize.width / 2 - size.getWidth() / 2), 0);
        cf.setVisible(true);
    }
}
