
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * A computer consisting of a {@link Processor} and a {@link Memory}.
 */
public class Computer {

    /**
     * Magical header used for programs to be able to perform a basic
     * sanity check that a file contains an executable program.
     */
    final static String MAGICAL_HEADER = "1f1f1f1f";

    /**
     * Processor of this computer.
     */
    protected Processor processor;

    /**
     * Memory of this computer.
     */
    protected Memory mem;

    /**
     * Delay between an execution and a fectch. This simulates the
     * clock tick in a real computer.
     */
    protected int clockTick = 125;

    /**
     * Determines if this computer is running non-interactively or
     * not.
     */
    protected Running running;

    /**
     * This computer in clocked form.
     */
    protected ClockedComputer clockedComputer;

    /**
     * Program stored by this computer.
     */
    protected String program;

    /**
     * Create an uninitialized instance. This is useful in subclasses.
     */
    protected Computer() {
    }

    /**
     * Creates an observable computer with the given parameters.
     *
     * @param MEMORYCELLS Size of memory in blocks.
     * @param BLOCKSIZE Blocksize of memory.
     * @param WORDBLOCKS Number of blocks in a word stored in general
     * purpose registers.
     * @param NOREGISTERS Number of registers. This must be
     * addressable by a block.
     */
    public Computer(final int MEMORYCELLS, final int BLOCKSIZE,
                    final int WORDBLOCKS, final int NOREGISTERS) {
        this.mem = new Memory(MEMORYCELLS, BLOCKSIZE);
        this.processor = new Processor(this.mem, WORDBLOCKS, NOREGISTERS);
        this.running = new GenericRunning(false);
    }

    /**
     * Creates an observable computer with 256 memory cells, 4-bit
     * bytesize, 2-byte words, and 16 registers and initializes it
     * with the given program.
     *
     * @param program Program to execute.
     * @throws NICException If the program can not be loaded.
     */
    public Computer(final String program) throws NICException {
        this(256, 4, 2, 16);
        setProgram(program);
    }

    /**
     * Returns the maximal length in hexadecimal digits of a program.
     *
     * @return Maximal length of program.
     */
    private int getMAXPROGRAMLENGTH() {
        return this.mem.getMEMORYCELLS() * this.mem.getBLOCKSIZE() / 4;
    }

    /**
     * Attempts to strip a magical header from the program and throws
     * an exception if it can not be found. In a real computer
     * something similar is typically done by the operating system.
     *
     * @param program Program.
     */
    private String stripHeader(final String program) {
        if (program.startsWith(MAGICAL_HEADER)) {
            return program.substring(MAGICAL_HEADER.length());
        } else {
            throw new Error("Wrong program format!");
        }
    }

    /**
     * Set the given program.
     *
     * @param programWithHeader Program as hexadecimal string.
     */
    public void setProgram(final String programWithHeader) throws NICException {
        final String program = stripHeader(programWithHeader);
        if (program.length() <= getMAXPROGRAMLENGTH()) {
            this.program = program;
        } else {
            throw new NICException("Too large program! ("
                                   + program.length() + " > "
                                   + getMAXPROGRAMLENGTH() + ")");
        }
    }

    /**
     * Returns the current program.
     *
     * @return Program currently stored in this computer.
     */
    public String getProgram() {
        return program;
    }

    /**
     * Generates a textual description of this computer.
     *
     * @return Description of this computer.
     */
    public String getDescription() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Frequency: " + (1000 / getClockTick()) + "Hz\n");
        sb.append("Memory: " + mem.getMEMORYCELLS() + " blocks\n");
        sb.append("Block: " + mem.getBLOCKSIZE() + " bits\n");
        sb.append("Word: " + processor.getWORDSIZE() + " bits\n");
        sb.append("Op-code: " + processor.getOPSIZE() + " bits\n");
        sb.append("Instruction: " + processor.getINSTRUCTIONSIZE() + " bits");
        return sb.toString();
    }

    /**
     * Set the clock tick of this computer.
     *
     * @param clockTick Clock tick between steps.
     */
    public void setClockTick(final int clockTick) {
        synchronized (this) {
            this.clockTick = clockTick;
        }
    }

    /**
     * Returns the clock tick of this computer.
     *
     * @return Clock tick of this computer.
     */
    public int getClockTick() {
        synchronized (this) {
            return clockTick;
        }
    }

    /**
     * Returns true or false depending on if this computer is running
     * or not.
     *
     * @return True or false depending on if this computer is running
     * or not.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Executes one step of the computer. Do nothing if the computer
     * is already executing in a thread.
     */
    public void step() {
        processor.step();
    }

    /**
     * Repeatedly steps the computer until a step is no longer
     * successful. Do nothing if the computer is executing in a
     * thread.
     */
    public void stepThrough() {
        while (getStatus() == Processor.SUCCESS) {
            step();
        }
    }

    /**
     * Load an input into the memory. The last word in the memory
     * points to the start of the input which is located as far up in
     * memory as possible.
     *
     * @param input Input given in hexadecimal.
     */
    public void loadInput(final String input) throws NICException {
        if (program == null) {
            throw new NICException("No program has been loaded!");
        } else {
            final int len = program.length() + input.length() + 2;
            if (len > getMAXPROGRAMLENGTH()) {
                final String e =
                    String.format("Input is too big! "
                                  + "(program + input is %s bytes)", len);
                throw new NICException(e);
            } else {
                final int address = mem.getMEMORYCELLS() - input.length() - 2;
                mem.load(address, input + String.format("%02x", address));
            }
        }
    }

    /**
     * Read an output from the memory. The last word in the memory
     * points to the start of the input which is located as far up in
     * memory as possible.
     *
     * @return Output given in hexadecimal.
     */
    public String readOutput() {
        final int len = mem.getMEMORYCELLS();
        final int p =
            Math.min((mem.get(len - 2) << 4) | mem.get(len - 1), len - 2);
        return mem.read(p, len - 2);
    }

    /**
     * Executes the program on the given input.
     *
     * @param input Input to program.
     * @return Output of the program when executed on the input.
     */
    public String execute(final String input)
        throws NICException {
        reset();
        loadInput(input);
        stepThrough();
        return readOutput();
    }

    /**
     * Executes the program on each line of input from the input
     * stream, and writes the results as lines on the destination
     * stream. Each line must be a hexadecimal string.
     *
     * @param is Source of inputs.
     * @param ps Destination of outputs.
     *
     * @throws NICException Issues encountered in NIC itself.
     * @throws IOException Issues with reading or writing data.
     */
    public void executeStream(final InputStream is, final PrintStream ps)
        throws NICException, IOException {
        final Scanner sc = new Scanner(is);
        while (sc.hasNextLine()) {
            ps.println(execute(sc.nextLine()));
        }
    }

    /**
     * Executes the program on each line of input from standard input,
     * and writes the results as lines on the standard output. Each
     * line must be a hexadecimal string.
     *
     * @param is Source of inputs.
     * @param ps Destination of outputs.
     *
     * @throws NICException Issues encountered in NIC itself.
     * @throws IOException Issues with reading or writing data.
     */
    public void executeStream() throws NICException, IOException {
        executeStream(System.in, System.out);
    }

    /**
     * Start the computer and execute from the current state. Do
     * nothing if the computer is already executing.
     */
    public void start() {
        synchronized (this) {
            if (!running.get()) {
                running.set(true);
                clockedComputer = new ClockedComputer(this, running);
                clockedComputer.start();
            }
        }
    }

    /**
     * Stop the computer. Do nothing if the computer is not executing.
     */
    public void stop() {
        synchronized (this) {
            if (running.get()) {
                try {
                    running.set(false);
                    clockedComputer.join();
                    clockedComputer = null;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * Turns on or off the power of this computer, i.e., if it is not
     * running, then it is started and otherwise it is stopped.
     */
    public void startStop() {
        if (running.get()) {
            stop();
        } else {
            start();
        }
    }

    /**
     * Returns true if the next move of this computer is fetch and
     * false otherwise.
     *
     * @return True or false depending on if fetches or executes next.
     */
    public boolean nextIsFetch() {
        return processor.getNext() == Processor.FETCH;
    }

    /**
     * Returns the status of the processor.
     *
     * @return Status of the processor.
     */
    public int getStatus() {
        return processor.getStatus();
    }

    /**
     * Stop and reset the processor, clear memory, and load the
     * program.
     */
    public void reset() {
        stop();
        synchronized (this) {
            processor.reset();
            mem.clear();
            try {
                mem.load(0, program);
            } catch (final NICException nice) {
                throw new Error("Program could not be reloaded!", nice);
            }
        }
    }
}

/**
 * Creates a clocked computer. This simulates the "clock circuit"
 * that wraps a physical computer.
 */
class ClockedComputer extends Thread {

    /**
     * Executed computer.
     */
    final Computer comp;

    /**
     * Signals if this thread should run or not.
     */
    final Running running;

    /**
     * Creates a clocked computer for the given computer.
     *
     * @param comp Executed computer.
     */
    ClockedComputer(final Computer comp, final Running running) {
        this.comp = comp;
        this.running = running;
    }

    @Override
    public void run() {
        try {

            // We execute until somebody stops the computer or until
            // the processor halts on its own.
            while (running.get() && comp.getStatus() == Processor.SUCCESS) {

                // Take one step.
                comp.step();

                // Wait for one "clock cycle" before fetching and
                // executing again.
                if (comp.nextIsFetch()) {
                    sleep(comp.getClockTick());
                }
            }
        } catch (InterruptedException e) {
        } finally {
            running.set(false);
        }
    }
}
