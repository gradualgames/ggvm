package com.gradualgames.ggvm;

import com.badlogic.gdx.Gdx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 8/21/2016.
 *
 * Represents the 6502 cpu of the NES. It knows about the Bus,
 * which maps to ram, rom, and hardware peripherals. Besides that
 * it hard codes reading the nmi, reset and irq vectors from the
 * vectors address and when reset is called, clears all cpu registers
 * and sets the pc to the value of reset. From there, the GGVm class
 * is expected to call nmi and also call execute repeatedly every frame,
 * enough times to approximate the performance of a real NES.
 *
 * TODO: Implement calculation of the overflow flag.
 * TODO: Finish implementation of indexed indirect addressing.
 */
public class Cpu {

    public static final int RAM_SIZE = 2048;

    public static final int VECTORS_ADDRESS = 0xfffa;

    public static final int EMERGENCY_NMI_BREAK_INSTRUCTION_COUNT = 900;

    private CpuBus bus;

    /**
     * Counts the number of instructions executed (for performance profiling)
     */
    private long instructionCount;
    private long startMillis;

    /**
     * The following members are all internal registers of the CPU.
     */
    private int pc;
    private int sp;
    private int a;
    private int x;
    private int y;

    /**
     * The following are all cpu status flags
     */
    private boolean status_interrupt_disable;
    private boolean status_decimal_mode;
    private int status_carry;
    private boolean status_negative;
    private boolean status_zero;
    private boolean status_overflow;

    /**
     * The following keep track of vectors obtained from hardcoded locations
     * on the bus.
     */
    private int nmi;
    private int reset;
    private int irq;

    public Cpu(CpuBus bus) {
        this.bus = bus;

        nmi = bus.readUnsignedWordAsInt(VECTORS_ADDRESS);
        reset = bus.readUnsignedWordAsInt(VECTORS_ADDRESS + 2);
        irq = bus.readUnsignedWordAsInt(VECTORS_ADDRESS + 4);
    }

    public void save(OutputStream outputStream) throws IOException {
        outputStream.write(pc & 0xff);
        outputStream.write(pc >> 8);
        outputStream.write(sp & 0xff);
        outputStream.write(sp >> 8);
        outputStream.write(a);
        outputStream.write(x);
        outputStream.write(y);
        outputStream.write(status_interrupt_disable ? 1: 0);
        outputStream.write(status_decimal_mode ? 1: 0);
        outputStream.write(status_carry);
        outputStream.write(status_negative ? 1: 0);
        outputStream.write(status_zero ? 1: 0);
        outputStream.write(status_overflow ? 1: 0);
    }

    public void load(InputStream inputStream) throws IOException {
        int lo = inputStream.read();
        int hi = inputStream.read();
        pc = (hi << 8) | lo;

        lo = inputStream.read();
        hi = inputStream.read();
        sp = (hi << 8) | lo;

        a = inputStream.read();
        x = inputStream.read();
        y = inputStream.read();
        status_interrupt_disable = inputStream.read() == 1 ? true : false;
        status_decimal_mode = inputStream.read() == 1 ? true : false;
        status_carry = inputStream.read();
        status_negative = inputStream.read() == 1 ? true : false;
        status_zero = inputStream.read() == 1 ? true : false;
        status_overflow = inputStream.read() == 1 ? true : false;
    }

    public void startTimer() {
        instructionCount = 0;
        startMillis = System.currentTimeMillis();
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public int instructionsPerSecond() {
        float seconds = (float) (System.currentTimeMillis() - startMillis) / 1000f;
        return (int) (instructionCount / seconds);
    }

    public void reset() {
        pc = reset;
    }

    public int getPc() {
        return pc;
    }

    public int getA() { return (byte) a; }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private void printByte(String label, int address) {
        Gdx.app.log(getClass().getSimpleName(), label + ": " + bus.readUnsignedByteAsInt(address));
    }

    private void printWord(String label, int address) {
        Gdx.app.log(getClass().getSimpleName(), label + ": "+ bus.readUnsignedWordAsInt(address));
    }

    public void printRegisters() {
        Gdx.app.log(getClass().getSimpleName(), "****************************************************************");
        Gdx.app.log(getClass().getSimpleName(), "CPU Status:");
        Gdx.app.log(getClass().getSimpleName(), "a: " + Integer.toHexString(a) + " x: " + Integer.toHexString(x) + " y: " + Integer.toHexString(y));
        Gdx.app.log(getClass().getSimpleName(), "pc: " + Integer.toHexString(pc));
        Gdx.app.log(getClass().getSimpleName(), "sp: " + Integer.toHexString(sp));
        Gdx.app.log(getClass().getSimpleName(), "C:" + status_carry);
        Gdx.app.log(getClass().getSimpleName(), "Z:" + status_zero);
        Gdx.app.log(getClass().getSimpleName(), "I:" + status_interrupt_disable);
        Gdx.app.log(getClass().getSimpleName(), "D:" + status_decimal_mode);
        Gdx.app.log(getClass().getSimpleName(), "V:" + status_overflow);
        Gdx.app.log(getClass().getSimpleName(), "N:" + status_negative);
        Gdx.app.log(getClass().getSimpleName(), "****************************************************************");
    }

    public String getRegistersString() {
        return " a: " + Integer.toHexString(a) +
               " x: " + Integer.toHexString(x) +
               " y: " + Integer.toHexString(y) +
               " pc: " + Integer.toHexString(pc) +
               " sp: " + Integer.toHexString(sp) +
               " C: " + status_carry +
               " Z: " + status_zero +
               " I: " + status_interrupt_disable +
               " D: " + status_decimal_mode +
               " V: " + status_overflow +
               " N: " + status_negative;
    }

    /**
     * "Interrupts" the main execution of the CPU by remembering the pc on the
     * stack, pushing also the status register, and then setting pc = nmi and
     * executing there until rti is encountered.
     */
    public void nmi() {
        //Perform same logic as jsr
        int returnPoint = pc - 1;
        int lo = returnPoint & 0xff;
        int hi = (returnPoint & 0xff00) >> 8;
        bus.writeIntAsByte(sp--, hi);
        bus.writeIntAsByte(sp--, lo);

        //Push processor flags, same logic as php
        int value = 0;
        if (status_carry == 1) {
            value = (1 << 6);
        }
        if (status_zero) {
            value |= (1 << 5);
        }
        if (status_interrupt_disable) {
            value |= (1 << 4);
        }
        if (status_decimal_mode) {
            value |= (1 << 3);
        }
        //TODO: Implement break mode flag?
        //TODO: This would be 1 << 2 here
        if (status_overflow) {
            value |= (1 << 1);
        }
        if (status_negative) {
            value |= 1;
        }

        bus.writeIntAsByte(sp--, value);

        //Now set pc to the nmi address
        pc = nmi;

        //Execute until rti (0x40) or we execute too long
        int instructions = EMERGENCY_NMI_BREAK_INSTRUCTION_COUNT;
        while(execute() != 0x40 && instructions-- > 0);
    }

    /**
     * Looks at the instruction located on the bus at the current program
     * counter, then jumps to the case associated with that opcode and performs
     * the associated logic.
     *
     * @return Returns the instruction that was executed (for use by nmi to determine
     * when rti has been executed).
     */
    public int execute() {

        int instruction = bus.readUnsignedByteAsInt(pc);

        int address;
        int value;
        int lo, hi;

//        Gdx.app.debug(getClass().getSimpleName(), "****************************************************************");
//        Gdx.app.debug(getClass().getSimpleName(), "Executing opcode: " + Integer.toHexString(instruction));

        switch(instruction) {
            //adc
            case 0x69: //adc #
                value = bus.readUnsignedByteAsInt(++pc);
                a += value + status_carry;
                //Check for overflow
                if (a > 0xff) {
                    status_carry = 1;
                    a -= 0x100;
                } else {
                    status_carry = 0;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc++;
                break;
            case 0x65: //adc zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                a += value + status_carry;
                //Check for overflow
                if (a > 0xff) {
                    status_carry = 1;
                    a -= 0x100;
                } else {
                    status_carry = 0;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc++;
                break;
            case 0x75: //adc zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                a += value + status_carry;
                //Check for overflow
                if (a > 0xff) {
                    status_carry = 1;
                    a -= 0x100;
                } else {
                    status_carry = 0;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc++;
                break;
            case 0x6d: //adc abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                a += value + status_carry;
                //Check for overflow
                if (a > 0xff) {
                    status_carry = 1;
                    a -= 0x100;
                } else {
                    status_carry = 0;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc += 2;
                break;
            case 0x7d: //adc abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                a += value + status_carry;
                //Check for overflow
                if (a > 0xff) {
                    status_carry = 1;
                    a -= 0x100;
                } else {
                    status_carry = 0;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc += 2;
                break;
            case 0x79: //adc abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + y);
                a += value + status_carry;
                //Check for overflow
                if (a > 0xff) {
                    status_carry = 1;
                    a -= 0x100;
                } else {
                    status_carry = 0;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc += 2;
                break;
            case 0x61: //adc (indirect,x)
                throw new RuntimeException("adc (indirect,x) not implemented!");
            case 0x71: //adc (indirect),y
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                value = bus.readUnsignedByteAsInt(address + y);
                a += value + status_carry;
                //Check for overflow
                if (a > 0xff) {
                    status_carry = 1;
                    a -= 0x100;
                } else {
                    status_carry = 0;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc++;
                break;

            //and
            case 0x29: //and #
                a &= bus.readUnsignedByteAsInt(++pc);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x25: //and zp
                address = bus.readUnsignedByteAsInt(++pc);
                a &= bus.readUnsignedByteAsInt(address);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x35: //and zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                a &= bus.readUnsignedByteAsInt(address + x);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x2d: //and abs
                address = bus.readUnsignedWordAsInt(++pc);
                a &= bus.readUnsignedByteAsInt(address);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0x3d: //and abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                a &= bus.readUnsignedByteAsInt(address + x);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0x39: //and abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                a &= bus.readUnsignedByteAsInt(address + y);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0x21: //and (indirect,x)
                throw new RuntimeException("and (indirect,x) not implemented!");
            case 0x31: //and (indirect),y
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                a &= bus.readUnsignedByteAsInt(address + y);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;

            //asl
            case 0x0a: //asl a
                a <<= 1;
                status_carry = (a & 0x100) >> 8;
                a &= 0xff;
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x06: //asl zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                value <<= 1;
                status_carry = (value & 0x100) >> 8;
                value &= 0xff;
                status_negative = (value & 0x80) == 0x80;
                status_zero = a == 0;
                bus.writeIntAsByte(address, value);
                pc++;
                break;
            case 0x16: //asl zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                value <<= 1;
                status_carry = (value & 0x100) >> 8;
                value &= 0xff;
                status_negative = (value & 0x80) == 0x80;
                status_zero = a == 0;
                bus.writeIntAsByte(address + x, value);
                pc++;
                break;
            case 0x0e: //asl abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                value <<= 1;
                status_carry = (value & 0x100) >> 8;
                value &= 0xff;
                status_negative = (value & 0x80) == 0x80;
                status_zero = a == 0;
                bus.writeIntAsByte(address, value);
                pc += 2;
                break;
            case 0x1e: //asl abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                value <<= 1;
                status_carry = (value & 0x100) >> 8;
                value &= 0xff;
                status_negative = (value & 0x80) == 0x80;
                status_zero = a == 0;
                bus.writeIntAsByte(address + x, value);
                pc += 2;
                break;

            //bcc
            case 0x90:
                if (status_carry == 0) {
                    pc += bus.readSignedByte(++pc);
                }
                pc += 2;
                break;

            //bcs
            case 0xb0:
                if (status_carry == 1) {
                    pc += bus.readSignedByte(++pc);
                }
                pc += 2;
                break;

            //beq
            case 0xf0:
                if (status_zero) {
                    pc += bus.readSignedByte(++pc);
                }
                pc += 2;
                break;

            //bit
            case 0x24: //bit zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                status_zero = (value & a) == 0;
                status_negative = (value & 0x80) == 0x80;
                status_overflow = (value & 0x40) == 0x40;
                pc++;
                break;
            case 0x2c: //bit abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                status_zero = (value & a) == 0;
                status_negative = (value & 0x80) == 0x80;
                status_overflow = (value & 0x40) == 0x40;
                pc += 2;
                break;

            //bmi
            case 0x30:
                if (status_negative) {
                    pc += bus.readSignedByte(++pc);
                }
                pc += 2;
                break;

            //bne
            case 0xd0:
                if (!status_zero) {
                    pc += bus.readSignedByte(++pc);
                }
                pc += 2;
                break;

            //bpl
            case 0x10:
                if (!status_negative) {
                    pc += bus.readSignedByte(++pc);
                }
                pc += 2;
                break;

            //brk
            case 0x00:
                printRegisters();
                throw new RuntimeException("brk not implemented!");

            //bvc:
            case 0x50:
                if (!status_overflow) {
                    pc += bus.readSignedByte(++pc);
                }
                pc += 2;
                break;

            //bvs:
            case 0x70:
                if (status_overflow) {
                    pc += bus.readSignedByte(++pc);
                }
                pc += 2;
                break;

            //clc
            case 0x18:
                status_carry = 0;
                pc++;
                break;

            //cld
            case 0xd8:
                status_decimal_mode = false;
                pc++;
                break;

            //cli
            case 0x58:
                status_interrupt_disable = false;
                pc++;
                break;

            //clv
            case 0xb8:
                status_overflow = false;
                pc++;
                break;

            //cmp
            case 0xc9: //cmp #
                value = bus.readUnsignedByteAsInt(++pc);
                //Unsigned comparison
                if (a >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) a >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = a == (value & 0xff);
                pc++;
                break;
            case 0xc5: //cmp zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                //Unsigned comparison
                if (a >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) a >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = a == (value & 0xff);
                pc++;
                break;
            case 0xd5: //cmp zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                //Unsigned comparison
                if (a >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) a >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = a == (value & 0xff);
                pc++;
                break;
            case 0xcd: //cmp abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                //Unsigned comparison
                if (a >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) a >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = a == (value & 0xff);
                pc += 2;
                break;
            case 0xdd: //cmp abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                //Unsigned comparison
                if (a >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) a >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = a == (value & 0xff);
                pc += 2;
                break;
            case 0xd9: //cmp abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + y);
                //Unsigned comparison
                if (a >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) a >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = a == (value & 0xff);
                pc += 2;
                break;
            case 0xc1: //cmp (indirect,x)
                throw new RuntimeException("cmp (indirect,x) not implemented!");
            case 0xd1: //cmp (indirect),y
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                value = bus.readUnsignedByteAsInt(address + y);
                //Unsigned comparison
                if (a >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) a >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = a == (value & 0xff);
                pc++;
                break;

            //cpx
            case 0xe0: //cpx #
                value = bus.readUnsignedByteAsInt(++pc);
                //Unsigned comparison
                if (x >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) x >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = x == (value & 0xff);
                pc++;
                break;
            case 0xe4: //cpx zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                //Unsigned comparison
                if (x >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) x >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = x == (value & 0xff);
                pc++;
                break;
            case 0xec: //cpx abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                //Unsigned comparison
                if (x >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) x >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = x == (value & 0xff);
                pc += 2;
                break;

            //cpy
            case 0xc0: //cpy #
                value = bus.readUnsignedByteAsInt(++pc);
                //Unsigned comparison
                if (y >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) y >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = y == (value & 0xff);
                pc++;
                break;
            case 0xc4: //cpy zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                //Unsigned comparison
                if (y >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) y >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = y == (value & 0xff);
                pc++;
                break;
            case 0xcc: //cpy abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                //Unsigned comparison
                if (y >= value) {
                    status_carry = 1;
                }  else {
                    status_carry = 0;
                }
                //Signed comparison
                value = (byte) value;
                if ((byte) y >= (byte) value) {
                    status_negative = false;
                }  else {
                    status_negative = true;
                }
                status_zero = y == (value & 0xff);
                pc += 2;
                break;

            //dec
            case 0xc6: //dec zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address) - 1;
                value &= 0xff;
                status_zero = value == 0;
                status_negative = (value & 0x80) == 0x80;
                bus.writeIntAsByte(address, value);
                pc++;
                break;
            case 0xd6: //dec zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x) - 1;
                value &= 0xff;
                status_zero = value == 0;
                status_negative = (value & 0x80) == 0x80;
                bus.writeIntAsByte(address + x, value);
                pc++;
                break;
            case 0xce: //dec abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address) - 1;
                value &= 0xff;
                status_zero = value == 0;
                status_negative = (value & 0x80) == 0x80;
                bus.writeIntAsByte(address, value);
                pc += 2;
                break;
            case 0xde: //dec abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x) - 1;
                value &= 0xff;
                status_zero = value == 0;
                status_negative = (value & 0x80) == 0x80;
                bus.writeIntAsByte(address + x, value);
                pc += 2;
                break;

            //dex
            case 0xca:
                x--;
                x &= 0xff;
                status_zero = x == 0;
                status_negative = (x & 0x80) == 0x80;
                pc++;
                break;

            //dey
            case 0x88:
                y--;
                y &= 0xff;
                status_zero = y == 0;
                status_negative = (y & 0x80) == 0x80;
                pc++;
                break;

            //eor:
            case 0x49: //eor #
                a ^= bus.readUnsignedByteAsInt(++pc);
                status_zero = a == 0;
                status_negative = (a & 0x80) == 0x80;
                pc++;
                break;
            case 0x45: //eor zp
                address = bus.readUnsignedByteAsInt(++pc);
                a ^= bus.readUnsignedByteAsInt(address);
                status_zero = a == 0;
                status_negative = (a & 0x80) == 0x80;
                pc++;
                break;
            case 0x55: //eor zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                a ^= bus.readUnsignedByteAsInt(address + x);
                status_zero = a == 0;
                status_negative = (a & 0x80) == 0x80;
                pc++;
                break;
            case 0x4d: //eor abs
                address = bus.readUnsignedWordAsInt(++pc);
                a ^= bus.readUnsignedByteAsInt(address);
                status_zero = a == 0;
                status_negative = (a & 0x80) == 0x80;
                pc += 2;
                break;
            case 0x5d: //eor abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                a ^= bus.readUnsignedByteAsInt(address + x);
                status_zero = a == 0;
                status_negative = (a & 0x80) == 0x80;
                pc += 2;
                break;
            case 0x59: //eor abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                a ^= bus.readUnsignedByteAsInt(address + y);
                status_zero = a == 0;
                status_negative = (a & 0x80) == 0x80;
                pc += 2;
                break;
            case 0x41: //eor (indirect,x)
                throw new RuntimeException("eor (indirect,x) not implemented!");
            case 0x51: //eor (indirect),y
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                a ^= bus.readUnsignedByteAsInt(address + y);
                status_zero = a == 0;
                status_negative = (a & 0x80) == 0x80;
                pc++;
                break;

            //inc
            case 0xe6: //inc zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address) + 1;
                value &= 0xff;
                status_zero = value == 0;
                status_negative = (value & 0x80) == 0x80;
                bus.writeIntAsByte(address, value);
                pc++;
                break;
            case 0xf6: //inc zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x) + 1;
                value &= 0xff;
                status_zero = value == 0;
                status_negative = (value & 0x80) == 0x80;
                bus.writeIntAsByte(address + x, value);
                pc++;
                break;
            case 0xee: //inc abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address) + 1;
                value &= 0xff;
                status_zero = value == 0;
                status_negative = (value & 0x80) == 0x80;
                bus.writeIntAsByte(address, value);
                pc += 2;
                break;
            case 0xfe: //inc abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x) + 1;
                value &= 0xff;
                status_zero = value == 0;
                status_negative = (value & 0x80) == 0x80;
                bus.writeIntAsByte(address + x, value);
                pc += 2;
                break;

            //inx
            case 0xe8:
                x++;
                x &= 0xff;
                status_zero = x == 0;
                status_negative = (x & 0x80) == 0x80;
                pc++;
                break;

            //iny
            case 0xc8:
                y++;
                y &= 0xff;
                status_zero = y == 0;
                status_negative = (y & 0x80) == 0x80;
                pc++;
                break;

            //jmp
            case 0x4c: //jmp abs
                address = bus.readUnsignedWordAsInt(++pc);
                pc = address;
                break;
            case 0x6c: //jmp (indirect)
                address = bus.readUnsignedWordAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                pc = address;
                break;

            //jsr
            case 0x20:
                int returnPoint = pc + 2;
                lo = returnPoint & 0xff;
                hi = (returnPoint & 0xff00) >> 8;
                bus.writeIntAsByte(sp--, hi);
                bus.writeIntAsByte(sp--, lo);
                pc = bus.readUnsignedWordAsInt(++pc);
                break;

            //lda
            case 0xa9: //lda #
                a = bus.readUnsignedByteAsInt(++pc);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0xa5: //lda zp
                address = bus.readUnsignedByteAsInt(++pc);
                a = bus.readUnsignedByteAsInt(address);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0xb5: //lda zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                a = bus.readUnsignedByteAsInt(address + x);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0xad: //lda abs
                address = bus.readUnsignedWordAsInt(++pc);
                a = bus.readUnsignedByteAsInt(address);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0xbd: //lda abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                a = bus.readUnsignedByteAsInt(address + x);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0xb9: //lda abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                a = bus.readUnsignedByteAsInt(address + y);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0xa1: //lda (indirect,x)
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address + x);
                a = bus.readUnsignedByteAsInt(address);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0xb1: // lda (indirect),y
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                a = bus.readUnsignedByteAsInt(address + y);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;

            //ldx
            case 0xa2: //ldx #
                x = bus.readUnsignedByteAsInt(++pc);
                status_negative = (x & 0x80) == 0x80;
                status_zero = x == 0;
                pc++;
                break;
            case 0xa6: //ldx zp
                address = bus.readUnsignedByteAsInt(++pc);
                x = bus.readUnsignedByteAsInt(address);
                status_negative = (x & 0x80) == 0x80;
                status_zero = x == 0;
                pc++;
                break;
            case 0xb6: //ldx zp,y
                address = bus.readUnsignedByteAsInt(++pc);
                x = bus.readUnsignedByteAsInt(address + y);
                status_negative = (x & 0x80) == 0x80;
                status_zero = x == 0;
                pc++;
                break;
            case 0xae: //ldx abs
                address = bus.readUnsignedWordAsInt(++pc);
                x = bus.readUnsignedByteAsInt(address);
                status_negative = (x & 0x80) == 0x80;
                status_zero = x == 0;
                pc += 2;
                break;
            case 0xbe: //ldx abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                x = bus.readUnsignedByteAsInt(address + y);
                status_negative = (x & 0x80) == 0x80;
                status_zero = x == 0;
                pc += 2;
                break;

            //ldy
            case 0xa0: //ldy #
                y = bus.readUnsignedByteAsInt(++pc);
                status_negative = (y & 0x80) == 0x80;
                status_zero = y == 0;
                pc++;
                break;
            case 0xa4: //ldy zp
                address = bus.readUnsignedByteAsInt(++pc);
                y = bus.readUnsignedByteAsInt(address);
                status_negative = (y & 0x80) == 0x80;
                status_zero = y == 0;
                pc++;
                break;
            case 0xb4: //ldy zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                y = bus.readUnsignedByteAsInt(address + x);
                status_negative = (y & 0x80) == 0x80;
                status_zero = y == 0;
                pc++;
                break;
            case 0xac: //ldy abs
                address = bus.readUnsignedWordAsInt(++pc);
                y = bus.readUnsignedByteAsInt(address);
                status_negative = (y & 0x80) == 0x80;
                status_zero = y == 0;
                pc += 2;
                break;
            case 0xbc: //ldy abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                y = bus.readUnsignedByteAsInt(address + x);
                status_negative = (y & 0x80) == 0x80;
                status_zero = y == 0;
                pc += 2;
                break;

            //lsr
            case 0x4a: //lsr a
                status_carry = a & 1;
                a >>= 1;
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x46: //lsr zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                status_carry = value & 1;
                value >>= 1;
                bus.writeIntAsByte(address, value);
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                pc++;
                break;
            case 0x56: //lsr zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                status_carry = value & 1;
                value >>= 1;
                bus.writeIntAsByte(address + x, value);
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                pc++;
                break;
            case 0x4e: //lsr abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                status_carry = value & 1;
                value >>= 1;
                bus.writeIntAsByte(address, value);
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                pc += 2;
                break;
            case 0x5e: //lsr abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                status_carry = value & 1;
                value >>= 1;
                bus.writeIntAsByte(address + x, value);
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                pc += 2;
                break;

            //nop
            case 0xea:
                pc++;
                break;

            //ora
            case 0x09: //ora #
                a |= bus.readUnsignedByteAsInt(++pc);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x05: //ora zp
                address = bus.readUnsignedByteAsInt(++pc);
                a |= bus.readUnsignedByteAsInt(address);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x15: //ora zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                a |= bus.readUnsignedByteAsInt(address + x);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x0d: //ora abs
                address = bus.readUnsignedWordAsInt(++pc);
                a |= bus.readUnsignedByteAsInt(address);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0x1d: //ora abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                a |= bus.readUnsignedByteAsInt(address + x);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0x19: //ora abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                a |= bus.readUnsignedByteAsInt(address + y);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc += 2;
                break;
            case 0x01: //ora (indirect,x)
                throw new RuntimeException("ora (indirect,x) not implemented!");
            case 0x11: //ora (indirect),y
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                a |= bus.readUnsignedByteAsInt(address + y);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;

            //pha
            case 0x48:
                bus.writeIntAsByte(sp--, a);
                pc++;
                break;

            //php
            case 0x08:
                value = 0;
                if (status_carry == 1) {
                    value = (1 << 6);
                }
                if (status_zero) {
                    value |= (1 << 5);
                }
                if (status_interrupt_disable) {
                    value |= (1 << 4);
                }
                if (status_decimal_mode) {
                    value |= (1 << 3);
                }
                //TODO: Implement break mode flag?
                //TODO: This would be 1 << 2 here
                if (status_overflow) {
                    value |= (1 << 1);
                }
                if (status_negative) {
                    value |= 1;
                }

                bus.writeIntAsByte(sp--, value);
                pc++;
                break;

            //pla
            case 0x68:
                a = bus.readUnsignedByteAsInt(++sp);
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;

            //plp
            case 0x28:
                value = bus.readUnsignedByteAsInt(++sp);
                if ((value & (1 << 6)) != 0) {
                    status_carry = 1;
                } else {
                    status_carry = 0;
                }
                if ((value & (1 << 5)) != 0) {
                    status_zero = true;
                } else {
                    status_zero = false;
                }
                if ((value & (1 << 4)) != 0) {
                    status_interrupt_disable = true;
                } else {
                    status_interrupt_disable = false;
                }
                if ((value & (1 << 3)) != 0) {
                    status_decimal_mode = true;
                } else {
                    status_decimal_mode = false;
                }
                //TODO: Implement break mode flag?
                //TODO: This would be 1 << 2 here
                if ((value & (1 << 1)) != 0) {
                    status_overflow = true;
                } else {
                    status_overflow = false;
                }
                if ((value & 1) != 0) {
                    status_negative = true;
                } else {
                    status_negative = false;
                }
                pc++;
                break;

            //rol
            case 0x2a: //rol a
                a <<= 1;
                a |= status_carry;
                status_carry = (a & (1 << 8)) >> 8;
                a &= 0xff;
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x26: //rol zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                value <<= 1;
                value |= status_carry;
                status_carry = (value & (1 << 8)) >> 8;
                value &= 0xff;
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                bus.writeIntAsByte(address, value);
                pc++;
                break;
            case 0x36: //rol zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                value <<= 1;
                value |= status_carry;
                status_carry = (value & (1 << 8)) >> 8;
                value &= 0xff;
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                bus.writeIntAsByte(address + x, value);
                pc++;
                break;
            case 0x2e: //rol abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                value <<= 1;
                value |= status_carry;
                status_carry = (value & (1 << 8)) >> 8;
                value &= 0xff;
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                bus.writeIntAsByte(address, value);
                pc += 2;
                break;
            case 0x3e: //rol abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                value <<= 1;
                value |= status_carry;
                status_carry = (value & (1 << 8)) >> 8;
                value &= 0xff;
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                bus.writeIntAsByte(address + x, value);
                pc += 2;
                break;

            //ror
            case 0x6a: //ror a
                a |= (status_carry << 8);
                status_carry = a & 1;
                a >>= 1;
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;
            case 0x66: //ror zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                value |= (status_carry << 8);
                status_carry = value & 1;
                value >>= 1;
                bus.writeIntAsByte(address, value);
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                pc++;
                break;
            case 0x76: //ror zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                value |= (status_carry << 8);
                status_carry = value & 1;
                value >>= 1;
                bus.writeIntAsByte(address + x, value);
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                pc++;
                break;
            case 0x6e: //ror abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                value |= (status_carry << 8);
                status_carry = value & 1;
                value >>= 1;
                bus.writeIntAsByte(address, value);
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                pc += 2;
                break;
            case 0x7e: //ror abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                value |= (status_carry << 8);
                status_carry = value & 1;
                value >>= 1;
                bus.writeIntAsByte(address + x, value);
                status_negative = (value & 0x80) == 0x80;
                status_zero = value == 0;
                pc += 2;
                break;

            //rti
            case 0x40:
                value = bus.readUnsignedByteAsInt(++sp);
                if ((value & (1 << 6)) != 0) {
                    status_carry = 1;
                } else {
                    status_carry = 0;
                }
                if ((value & (1 << 5)) != 0) {
                    status_zero = true;
                } else {
                    status_zero = false;
                }
                if ((value & (1 << 4)) != 0) {
                    status_interrupt_disable = true;
                } else {
                    status_interrupt_disable = false;
                }
                if ((value & (1 << 3)) != 0) {
                    status_decimal_mode = true;
                } else {
                    status_decimal_mode = false;
                }
                //TODO: Implement break mode flag?
                //TODO: This would be 1 << 2 here
                if ((value & (1 << 1)) != 0) {
                    status_overflow = true;
                } else {
                    status_overflow = false;
                }
                if ((value & 1) != 0) {
                    status_negative = true;
                } else {
                    status_negative = false;
                }
                lo = bus.readUnsignedByteAsInt(++sp);
                hi = bus.readUnsignedByteAsInt(++sp);
                pc = ((hi << 8) | lo) + 1;
                break;

            //rts
            case 0x60:
                lo = bus.readUnsignedByteAsInt(++sp);
                hi = bus.readUnsignedByteAsInt(++sp);
                pc = ((hi << 8) | lo) + 1;
                break;

            //sbc
            case 0xe9: //sbc #
                value = bus.readUnsignedByteAsInt(++pc);
                a = a - value - (1 - status_carry);
                if (a < 0) {
                    a &= 0xff;
                    status_carry = 0;
                } else {
                    status_carry = 1;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc++;
                break;
            case 0xe5: //sbc zp
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                a = a - value - (1 - status_carry);
                if (a < 0) {
                    a &= 0xff;
                    status_carry = 0;
                } else {
                    status_carry = 1;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc++;
                break;
            case 0xf5: //sbc zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                a = a - value - (1 - status_carry);
                if (a < 0) {
                    a &= 0xff;
                    status_carry = 0;
                } else {
                    status_carry = 1;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc++;
                break;
            case 0xed: //sbc abs
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address);
                a = a - value - (1 - status_carry);
                if (a < 0) {
                    a &= 0xff;
                    status_carry = 0;
                } else {
                    status_carry = 1;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc += 2;
                break;
            case 0xfd: //sbc abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + x);
                a = a - value - (1 - status_carry);
                if (a < 0) {
                    a &= 0xff;
                    status_carry = 0;
                } else {
                    status_carry = 1;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc += 2;
                break;
            case 0xf9: //sbc abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                value = bus.readUnsignedByteAsInt(address + y);
                a = a - value - (1 - status_carry);
                if (a < 0) {
                    a &= 0xff;
                    status_carry = 0;
                } else {
                    status_carry = 1;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc += 2;
                break;
            case 0xe1: //sbc (indirect,x)
                throw new RuntimeException("sbc (indirect,x) not implemented!");
            case 0xf1: //sbc (indirect),y
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                value = bus.readUnsignedByteAsInt(address + y);
                a = a - value - (1 - status_carry);
                if (a < 0) {
                    a &= 0xff;
                    status_carry = 0;
                } else {
                    status_carry = 1;
                }
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                //TODO: Implement overflow flag? I never use it.
                pc++;
                break;

            //sec
            case 0x38:
                status_carry = 1;
                pc++;
                break;

            //sed
            case 0xf8:
                status_decimal_mode = true;
                pc++;
                break;

            //sei
            case 0x78:
                status_interrupt_disable = true;
                pc++;
                break;

            //sta
            case 0x85: //sta zp
                address = bus.readUnsignedByteAsInt(++pc);
                bus.writeIntAsByte(address, a);
                pc++;
                break;
            case 0x95: //sta zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                bus.writeIntAsByte(address + x, a);
                pc++;
                break;
            case 0x8d: //sta abs
                address = bus.readUnsignedWordAsInt(++pc);
                bus.writeIntAsByte(address, a);
                pc += 2;
                break;
            case 0x9d: //sta abs,x
                address = bus.readUnsignedWordAsInt(++pc);
                bus.writeIntAsByte(address + x, a);
                pc += 2;
                break;
            case 0x99: //sta abs,y
                address = bus.readUnsignedWordAsInt(++pc);
                bus.writeIntAsByte(address + y, a);
                pc += 2;
                break;
            case 0x81: //sta (indirect,x)
                throw new RuntimeException("sta (indirect,x) not implemented!");
            case 0x91: // sta (indirect),y
                address = bus.readUnsignedByteAsInt(++pc);
                address = bus.readUnsignedWordAsInt(address);
                bus.writeIntAsByte(address + y, a);
                pc++;
                break;

            //stx
            case 0x86: //stx zp
                address = bus.readUnsignedByteAsInt(++pc);
                bus.writeIntAsByte(address, x);
                pc++;
                break;
            case 0x96: //stx zp,y
                address = bus.readUnsignedByteAsInt(++pc);
                bus.writeIntAsByte(address + y, x);
                pc++;
                break;
            case 0x8e: //stx abs
                address = bus.readUnsignedWordAsInt(++pc);
                bus.writeIntAsByte(address, x);
                pc += 2;
                break;

            //sty
            case 0x84: //sty zp
                address = bus.readUnsignedByteAsInt(++pc);
                bus.writeIntAsByte(address, y);
                pc++;
                break;
            case 0x94: //sty zp,x
                address = bus.readUnsignedByteAsInt(++pc);
                bus.writeIntAsByte(address + x, y);
                pc++;
                break;
            case 0x8c: //sty abs
                address = bus.readUnsignedWordAsInt(++pc);
                bus.writeIntAsByte(address, y);
                pc += 2;
                break;

            //tax
            case 0xaa:
                x = a;
                status_negative = (x & 0x80) == 0x80;
                status_zero = x == 0;
                pc++;
                break;

            //tay
            case 0xa8:
                y = a;
                status_negative = (y & 0x80) == 0x80;
                status_zero = y == 0;
                pc++;
                break;

            //tsx
            case 0xba:
                x = sp;
                status_negative = (x & 0x80) == 0x80;
                status_zero = x == 0;
                pc++;
                break;

            //txa
            case 0x8a:
                a = x;
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;

            //txs:
            case 0x9a:
                sp = 0x100 + x;
                pc++;
                break;

            //tya:
            case 0x98:
                a = y;
                status_negative = (a & 0x80) == 0x80;
                status_zero = a == 0;
                pc++;
                break;

            default:
                printRegisters();
                Gdx.app.error(getClass().getSimpleName(), "Instruction: " + instruction + " not implemented.");
                throw new RuntimeException("Instruction: " + instruction + " not implemented.");
        }
        instructionCount++;
        return instruction;
    }
}
