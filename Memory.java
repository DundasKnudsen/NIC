
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

/**
 * A random access memory, consisting of individual memory cells. This
 * simulates the internal memory of a physical computer formed as a
 * sequence of memory cells. Each memory cell is located at a given
 * index. This is called the address of the memory.
 */
public class Memory {

    /**
     * Underlying memory cells.
     */
    protected MemoryCell[] cells;

    /**
     * Bit-size of a block. In a modern physical computer this is
     * almost always 8, but in this simulator it may have a different
     * size. To avoid confusion we say that this is a "block" instead
     * of a "byte".
     */
    protected int BLOCKSIZE;

    /**
     * Number of hexadecimals used to represent one block.
     */
    protected int HEXSIZE;

    /**
     * Creates a partially uninitialized instance. This may be used in
     * subclasses.
     *
     * @param BLOCKSIZE Bit-size of a block.
     */
    protected Memory(final int BLOCKSIZE) {
        if (BLOCKSIZE <= 0) {
            throw new Error("Invalid block size! Must be positive. ("
                            + BLOCKSIZE + ")");
        }
        if (BLOCKSIZE % 4 != 0) {
            throw new Error("Invalid block size! Must be a multiple of four. ("
                            + BLOCKSIZE + ")");
        }
        this.HEXSIZE = BLOCKSIZE / 4;
        this.BLOCKSIZE = BLOCKSIZE;
    }

    /**
     * Creates a memory with the given number of memory cells, where
     * each memory cell is initialized to zero.
     *
     * @param MEMORYCELLS Number of memory cells.
     * @param BLOCKSIZE Bit-size of a block.
     */
    public Memory(final int MEMORYCELLS, final int BLOCKSIZE) {
        this(BLOCKSIZE);
        this.cells = new MemoryCell[MEMORYCELLS];
        for (int i = 0; i < this.cells.length; i++) {
            cells[i] = new GenericMemoryCell(BLOCKSIZE);
        }
    }

    /**
     * Stores a block at the given address in the memory. Addresses
     * are reduced modulo the number of memory cells.
     *
     * @param p Address in memory.
     * @param block Block to store.
     */
    public void set(final int p, final int block) {
        synchronized (this) {
            cells[p % this.cells.length].set(block);
        }
    }

    /**
     * Returns the block stored at the given address in the
     * memory. Addresses are reduced modulo the number of memory
     * cells.
     *
     * @param p Address in memory.
     * @return Block stored at the given address.
     */
    public int get(int p) {
        synchronized (this) {
            return cells[p % this.cells.length].get();
        }
    }

    /**
     * Returns the block size of each memory cell of this memory.
     *
     * @return Block size of memory cells in this memory.
     */
    public int getBLOCKSIZE() {
        return BLOCKSIZE;
    }

    /**
     * Returns the number of memory cells in this memory.
     *
     * @return Number of memory cells in this memory.
     */
    public int getMEMORYCELLS() {
        return cells.length;
    }

    /**
     * Sets all cells in this memory to zero.
     */
    public void clear() {
        for (int i = 0; i < this.cells.length; i++) {
            set(i, 0);
        }
    }

    /**
     * Converts a hexadecimal character to an integer.
     *
     * @param h Hexadecimal character.
     * @return Integer value.
     * @throws NICException If the input is not a hexadecimal string.
     */
    private int hexToInt(final char h) throws NICException {

        // Here we exploit that in Java a character can be viewed as
        // an integer, so what we get from '0' is the ASCII code as an
        // integer and ASCII encodes digits and letters in sequence.
        if ('0' <= h && h <= '9') {
            return h - '0';
        } else if ('a' <= h && h <= 'f') {
            return 10 + h - 'a';
        } else if ('A' <= h && h <= 'F') {
            return 10 + h - 'A';
        } else {

            // This will never happen if this function is used
            // properly.
            throw new NICException("The character is not a hexadecimal digit!");
        }
    }

    /**
     * Converts a hexadecimal string to an array of corresponding
     * integers.
     *
     * @param hexString Hexadecimal string.
     * @return Array of integer values.
     * @throws NICException If the input is not a hexadecimal string.
     */
    private int[] hexToIntArray(final String hexString) throws NICException {
        int[] res = new int[hexString.length()];
        for (int i = 0; i < hexString.length(); i++) {
            res[i] = hexToInt(hexString.charAt(i));
        }
        return res;
    }

    /**
     * Loads the input represented in hexadecimal form into memory
     * starting at the given destination address.
     *
     * @param p Destination address.
     * @param hexString Hexadecimal string.
     * @throws NICException If the input is not a hexadecimal string
     * or if it is too long.
     */
    public void load(int p, String hexString) throws NICException {
        final int[] values = hexToIntArray(hexString);

        // Verify that we can get complete blocks.
        if (values.length % HEXSIZE == 0) {

            // Number of blocks to compile.
            final int blocks = values.length / HEXSIZE;

            int j = 0;
            for (int i = 0; i < blocks; i++) {

                // Compile and set a block.
                int block = 0;
                for (int l = 0; l < HEXSIZE; l++) {

                    // Shift what we have so far to make room.
                    block <<= 4;

                    // Insert next 4 bits.
                    block |= values[j];
                    j++;
                }
                set(p + i, block);
            }
        } else {

            // This will never happen if this function is used
            // properly.
            throw new Error("Hex string is not an even multiple of blocks!");
        }
    }

    /**
     * Reads the blocks between the start and end addresses and
     * returns the contiguous sequence encoded in
     * hexadecimal. Addresses are reduced modulo the number of memory
     * cells and memory is interpreted as a circular buffer.
     *
     * @param p Inclusive start address.
     * @param q Exclusive end address.
     * @return Hexadecimal representation of the array.
     */
    public String read(final int p, final int q) {
        final StringBuilder sb = new StringBuilder();

        final int s = p % this.cells.length;
        final int e = q % this.cells.length;

        for (int i = s; i != e; i++) {
            sb.append(String.format("%x", get(i)));
        }
        return sb.toString();
    }
}
