
/**
 * Copyright 1997-2015 Stefan Nilsson, 2015-2017 Douglas Wikstrom.
 * This file is part of the NIC/NAS software licensed under BSD
 * License 2.0. See LICENSE file.
 */

package se.kth.csc.nic;

/* A simple processor with 16 general purpose registers, a program
 * counter (pc), and an instruction register (ir). When the processor
 * is constructed it is connected to a memory. There are two methods
 * for interacting with the processor:
 *
 * <ul>
 *
 * <li> {@link #fetch()} fetches the next instruction from the memory
 *      into ir and increments pc.
 *
 * <li> {@link #execute()} executes the instruction currently in ir.
 *
 * </ul>
 *
 * These methods return the following status codes:
 *
 * <ul>
 *
 * <li> SUCCESS: the method completed successfully.
 *
 * <li> HALT: a halt-instruction was encountered.
 *
 * <li> BAD_INSTRUCTION: an incorrect instruction was encountered.
 *
 * <li> BAD_ALIGNMENT: an attempt to read or write at an unaligned position.
 *
 * </ul>
 */
public class Processor {

    // Operator codes as blocks. These are the first parts of
    // instructions and tell the processor how to operate on the
    // following blocks.
    final static int opHalt      = 0x0;
    final static int opLoadMem   = 0x1;
    final static int opLoadConst = 0x2;
    final static int opLoadReg   = 0x3;
    final static int opStore     = 0x4;
    final static int opStoreReg  = 0x5;
    final static int opMove      = 0x6;
    final static int opAddInt    = 0x7;
    final static int opAddConst  = 0x8;
    final static int opMulInt    = 0x9;
    final static int opSubInt    = 0xa;
    final static int opRShift    = 0xb;
    final static int opAND       = 0xc;
    final static int opOR        = 0xd;
    final static int opXOR       = 0xe;
    final static int opJump      = 0xf;

    // Status codes. This corresponds to interrupts encountered by a
    // physical computer. Errors should never occur with a binary
    // executable that has been compiled from correct assembler code.
    public final static int SUCCESS             = 0x0;
    public final static int HALT                = 0x1;
    public final static int BAD_INSTRUCTION     = 0x2;
    public final static int BAD_ALIGNMENT       = 0x3;
    public final static int INTERRUPTED         = 0x4;

    // Next move of the processor.
    final static int FETCH               = 0x0;
    final static int EXEC                = 0x1;

    /**
     * Bit-size of a block on this machine. In a modern physical
     * computer this equals 8 and is called a "byte".
     */
    protected final int BLOCKSIZE;

    /**
     * Block-size of a word.
     */
    protected final int WORDBLOCKS;

    /**
     * Block-size of an operation code. Since we have 16 operation
     * codes and we know that each block is at least 4 bits, we set
     * this to one.
     */
    protected final int OPBLOCKS = 1;

    // The following constants are derived from the above.

    /**
     * Block where all bits equal one.
     */
    protected final int BLOCKMASK;

    /**
     * Bit-size of a word.
     */
    protected final int WORDSIZE;

    /**
     * Only top bit set of word.
     */
    protected final int WORDTOP;

    /**
     * Word where all bits equal one.
     */
    protected final int WORDMASK;

    /**
     * Bit-size of an operation code.
     */
    protected final int OPSIZE;

    /**
     * Operation code bitmask.
     */
    protected final int OPMASK;

    /**
     * Block-size of a complete instruction.
     */
    protected final int INSTRUCTIONBLOCKS;

    /**
     * Number of registers. This is always at most 2^BLOCKSIZE, since
     * this allows the content of a memory cell to address all the
     * registers.
     */
    protected int NOREGISTERS;

    /**
     * General purpose registers store a single word each.
     */
    protected Register[] reg;

    /**
     * Program counter register stores two words, and points to
     * current instruction.
     */
    protected Register pc;

    /**
     * Instruction register stores three words and an operation code.
     */
    protected Register ir;

    /**
     * Next register that determines if the next move is a fetch or an
     * execute.
     */
    protected Register nr;

    /**
     * Status register that stores the status code of the most recent
     * fetch or execute.
     */
    protected Register sr;

    /**
     * Memory of this computer, where each cell stores a single block.
     */
    protected Memory mem;

    /**
     * Creates a processor with the given memory, but without
     * registers.
     *
     * @param mem Underlying memory.
     * @param WORDBLOCKS Number of blocks in a word stored in general
     * purpose registers.
     */
    protected Processor(final Memory mem, final int WORDBLOCKS) {

        this.mem = mem;
        this.BLOCKSIZE = mem.getBLOCKSIZE();

        if (WORDBLOCKS <= 0) {
            throw new Error("Word blocks is non-positive! ("
                            + WORDBLOCKS + ")");
        }
        this.WORDBLOCKS = WORDBLOCKS;

        // Derive "constants" from BLOCKSIZE, WORDBLOCKS, AND OPBLOCKS.
        BLOCKMASK = (1 << BLOCKSIZE) - 1;

        WORDSIZE = WORDBLOCKS * BLOCKSIZE;
        WORDMASK = (1 << WORDSIZE) - 1;
        WORDTOP = 1 << (WORDSIZE - 1);

        OPSIZE = OPBLOCKS * BLOCKSIZE;
        OPMASK = (1 << OPSIZE) - 1;
        INSTRUCTIONBLOCKS = OPBLOCKS + 1 + WORDBLOCKS;

        if (mem.getMEMORYCELLS() > 1 << WORDSIZE) {
            throw new Error("All memory cells can not be addressed!");
        }
    }

    /**
     * Creates a processor with the given components.
     *
     * @param mem Underlying memory.
     * @param WORDBLOCKS Number of blocks in a word stored in general
     * purpose registers.
     * @param NOREGISTERS Number of registers. This must be
     * addressable by a block.
     */
    public Processor(final Memory mem, final int WORDBLOCKS,
                     final int NOREGISTERS) {
        this(mem, WORDBLOCKS);

        if (NOREGISTERS > 1 << BLOCKSIZE) {
            throw new Error("All registers can not be addressed!");
        }
        this.NOREGISTERS = NOREGISTERS;

        reg = new GenericRegister[NOREGISTERS];
        for (int i = 0; i < NOREGISTERS; i++) {
            reg[i] = new GenericRegister(WORDSIZE);
        }
        pc = new GenericRegister(WORDSIZE);
        ir = new GenericRegister(INSTRUCTIONBLOCKS * BLOCKSIZE);
        nr = new GenericRegister(WORDSIZE);
        sr = new GenericRegister(WORDSIZE);
    }

    /**
     * Returns the word size of this processor in bits.
     *
     * @return Word size of this processor.
     */
    public int getWORDSIZE() {
        return WORDSIZE;
    }

    /**
     * Returns the operation code size of this processor in bits.
     *
     * @return Word size of this processor.
     */
    public int getOPSIZE() {
        return OPSIZE;
    }

    /**
     * Returns the instruction size of this processor in bits.
     *
     * @return Word size of this processor.
     */
    public int getINSTRUCTIONSIZE() {
        return INSTRUCTIONBLOCKS * BLOCKSIZE;
    }

    /**
     * Returns the number of registers of this processor.
     *
     * @return Number of registers of this processor.
     */
    public int getNOREGISTERS() {
        return NOREGISTERS;
    }

    /**
     * Returns the next move of this processor.
     *
     * @return Next move of this processor.
     */
    public int getNext() {
        return nr.get();
    }

    /**
     * Returns the status of this processor.
     *
     * @return Status of this processor.
     */
    public int getStatus() {
        return sr.get();
    }

    /**
     * Reset this processor, i.e., set all registers to zero.
     */
    public void reset() {
        pc.set(0);
        ir.set(0);
        sr.set(0);
        for (int i = 0; i < reg.length; ++i) {
            reg[i].set(0);
        }
    }

    /**
     * Read the given number of blocks from memory starting at the
     * given pointer and return the result as an integer.
     *
     * @param p Pointer in memory.
     * @param blocks Number of blocks to read.
     * @return Blocks as an integer.
     */
    private int readInt(final int p, final int blocks) {

        // Read blocks from memory and form an integer.
        int res = 0;
        for (int i = 0; i < blocks; i++) {
            res |= mem.get(p + i);
            res <<= BLOCKSIZE;
        }
        return res >> BLOCKSIZE;
    }

    /**
     * Write the an integer as the given number of blocks to memory
     * starting at the given pointer.
     *
     * @param p Pointer in memory.
     * @param value Integer.
     * @param blocks Number of blocks to write.
     */
    private void writeInt(final int p, final int value, final int blocks) {

        for (int i = 0; i < blocks; i++) {
            int offset = (blocks - 1 - i) * BLOCKSIZE;
            int block = (value >> offset) & BLOCKMASK;
            mem.set(p + i, block);
        }
    }

    /**
     * Retrieves the next operation from memory and then increments the
     * program counter.
     */
    public void fetch() {

        // Program counter as pointer.
        int p = pc.get();

        // Check that the pointer is an even multiple of the
        // instruction size in blocks.
        if (p % INSTRUCTIONBLOCKS == 0) {

            // Read instruction into instruction register.
            ir.set(readInt(p, INSTRUCTIONBLOCKS));

            // Update program counter.
            pc.set((p + INSTRUCTIONBLOCKS) % mem.getMEMORYCELLS());

            sr.set(SUCCESS);

        } else {
            sr.set(BAD_ALIGNMENT);
        }
    }

    /**
     * Decode the bit pattern in the instruction register and execute
     * the command.
     */
    public void execute() {

        int instruction = ir.get();

        // Extract operator code and three words from the
        // instruction.
        int field3 = instruction & BLOCKMASK;
        instruction >>>= BLOCKSIZE;

        int field2 = instruction & BLOCKMASK;
        instruction >>>= BLOCKSIZE;

        int field1 = instruction & BLOCKMASK;
        instruction >>>= BLOCKSIZE;

        int opCode = instruction & OPMASK;

        switch (opCode) {
        case opHalt:
            halt();
            break;
        case opLoadMem:
            loadMem(field1, (field2 << BLOCKSIZE) + field3);
            break;
        case opLoadConst:
            loadConst(field1, (field2 << BLOCKSIZE) + field3);
            break;
        case opLoadReg:
            loadReg(field2, field3);
            break;
        case opStore:
            storeMem(field1, (field2 << BLOCKSIZE) + field3);
            break;
        case opStoreReg:
            storeReg(field2, field3);
            break;
        case opMove:
            move(field2, field3);
            break;
        case opAddInt:
            addInt(field1, field2, field3);
            break;
        case opAddConst:
            addConst(field1, (field2 << BLOCKSIZE) + field3);
            break;
        case opMulInt:
            mulInt(field1, field2, field3);
            break;
        case opSubInt:
            subInt(field1, field2, field3);
            break;
        case opRShift:
            shift(field1, field2, field3);
            break;
        case opAND:
            AND(field1, field2, field3);
            break;
        case opOR:
            OR(field1, field2, field3);
            break;
        case opXOR:
            XOR(field1, field2, field3);
            break;
        case opJump:
            jump(field1, (field2 << BLOCKSIZE) + field3);
            break;
        default:
            sr.set(BAD_INSTRUCTION);
        }
    }

    /**
     * Steps the processor, i.e., carries out either a fetch or an
     * execute as appropriate.
     */
    public void step() {
        if (nr.get() == FETCH) {
            fetch();
            nr.set(EXEC);
        } else {
            execute();
            nr.set(FETCH);
        }
    }

    /**
     * Loads the given destination register with the value stored in
     * memory pointed at by the source pointer register.
     *
     * @param d Index of destination register.
     * @param p Index of source pointer register.
     */
    void loadMem(final int d, final int p) {
        if (p % WORDBLOCKS != 0) {
            sr.set(BAD_ALIGNMENT);
        } else {
            reg[d].set(readInt(p, WORDBLOCKS));
            sr.set(SUCCESS);
        }
    }

    /**
     * Loads the given destination register with a constant.
     *
     * @param d Index of destination register.
     * @param c Constant.
     */
    void loadConst(final int d, final int c) {
        reg[d].set(c);
        sr.set(SUCCESS);
    }

    /**
     * Loads the given destination register with the value stored in
     * memory pointed at by the source pointer register.
     *
     * @param d Index of destination register.
     * @param s Index of source pointer register.
     */
    void loadReg(final int d, final int s) {
        int p = reg[s].get();
        loadMem(d, p);
    }

    /**
     * Stores the word in the source register in the blocks starting
     * at the pointer in the memory.
     *
     * @param s Index of source register.
     * @param p Destination pointer.
     */
    void storeMem(final int s, final int p) {
        if (p % WORDBLOCKS != 0) {
            sr.set(BAD_ALIGNMENT);
        } else {
            writeInt(p, reg[s].get(), WORDBLOCKS);
            sr.set(SUCCESS);
        }
    }

    /**
     * Stores the word in the source register in the blocks starting
     * at the value of the destination register in the memory.
     *
     * @param s Index of source register.
     * @param d Index of destination pointer register.
     */
    void storeReg(final int s, final int d) {
        storeMem(s, reg[d].get());
    }

    /**
     * Move word from source register to destination register.
     *
     * @param s Index of source register.
     * @param d Index of destination register.
     */
    void move(final int s, final int d) {
        reg[d].set(reg[s].get());
        sr.set(SUCCESS);
    }

    /**
     * Computes the sum of the integers in the source registers and
     * stores the result in the destination register.
     *
     * @param d Index of destination register.
     * @param a Index of source register.
     * @param b Index of source register.
     */
    void addInt(final int d, final int a, final int b) {
        reg[d].set(reg[a].get() + reg[b].get());
        sr.set(SUCCESS);
    }

    /**
     * Computes the sum of the integer in the destination register and
     * a constant word and stores the result in the destination
     * register.
     *
     * @param d Index of destination register.
     * @param c Constant word.
     */
    void addConst(final int d, final int c) {
        reg[d].set(reg[d].get() + c);
        sr.set(SUCCESS);
    }

    /**
     * Computes the product of the integers in the source registers and
     * stores the result in the destination register.
     *
     * @param d Index of destination register.
     * @param a Index of source register.
     * @param b Index of source register.
     */
    void mulInt(final int d, final int a, final int b) {
        reg[d].set(reg[a].get() * reg[b].get());
        sr.set(SUCCESS);
    }

    /**
     * Computes the difference of the integers in the source registers
     * and stores the result in the destination register.
     *
     * @param d Index of destination register.
     * @param a Index of source register.
     * @param b Index of source register.
     */
    void subInt(final int d, final int a, final int b) {
        reg[d].set((reg[a].get() - reg[b].get()));
        sr.set(SUCCESS);
    }

    /**
     * Shifts the integer in the first source register by the amount
     * specified in the second source register and stores the result
     * in the destination register, i.e., it sets r[d] = r[a] >>
     * abs(r[b]) or r[d] = r[a] << abs(r[b]) depending on if r[b] is
     * positive or negative.
     *
     * @param d Index of destination register.
     * @param a Index of source register.
     * @param b Index of source register.
     */
    void shift(final int d, final int a, final int b) {
        int rs = reg[a].get();
        int rt = reg[b].get();

        rt = signExtended(rt);
        if (rt > 0) {

            // For negative numbers we need to shift in additional
            // ones.
            rs = signExtended(rs);
            rs >>= rt;
        } else {
            rs <<= rt;
        }
        reg[d].set(rs & WORDMASK);

        sr.set(SUCCESS);
    }

    /**
     * Computes the bit-wise conjunction of the integers in the source
     * registers and stores the result in the destination register.
     *
     * @param d Index of destination register.
     * @param a Index of source register.
     * @param b Index of source register.
     */
    void AND(final int d, final int a, final int b) {
        reg[d].set(reg[a].get() & reg[b].get());
        sr.set(SUCCESS);
    }

    /**
     * Computes the bit-wise disjunction of the integers in the source
     * registers and stores the result in the destination register.
     *
     * @param d Index of destination register.
     * @param a Index of source register.
     * @param b Index of source register.
     */
    void OR(final int d, final int a, final int b) {
        reg[d].set(reg[a].get() | reg[b].get());
        sr.set(SUCCESS);
    }

    /**
     * Computes the bit-wise exclusive-or of the integers in the
     * source registers and stores the result in the destination
     * register.
     *
     * @param d Index of destination register.
     * @param a Index of source register.
     * @param b Index of source register.
     */
    void XOR(final int d, final int a, final int b) {
        reg[d].set(reg[a].get() ^ reg[b].get());
        sr.set(SUCCESS);
    }

    /**
     * Convert a word to an int using sign extension.
     *
     * @param word Word.
     * @return Int with the right sign.
     */
    int signExtended(final int word) {
        if ((word & WORDTOP) != 0) {
            return (0xFFFFFFFF << WORDSIZE) | word;
        } else {
            return word;
        }
    }

    /* Jump to memory cell a if the bit pattern in register r
     * eq/neq/le/leq the bit pattern in register 0.
     */
    void jump(final int r, final int a) {

        // Extract two least significant bits that encode which jump
        // instruction to use.
        final int b = a % 4;

        // Set the two least significant bits in address to zero.
        final int adr = a & (WORDMASK << 2);

        final int rx = signExtended(reg[r].get());
        final int r0 = signExtended(reg[0].get());

        switch (b) {
        case 0:
            if (rx == r0) {
                pc.set(adr);
            }
            break;
        case 1:
            if (rx != r0) {
                pc.set(adr);
            }
            break;
        case 2:
            if (rx < r0) {
                pc.set(adr);
            }
            break;
        case 3:
            if (rx <= r0) {
                pc.set(adr);
            }
            break;
        }
        sr.set(SUCCESS);
    }

    /**
     * Halt execution.
     */
    void halt() {
        sr.set(HALT);
    }
}
