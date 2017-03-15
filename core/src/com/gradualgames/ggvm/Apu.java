package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 9/3/2016.
 *
 * This is a dummy APU module. All it does is capture reads and writes where it
 * is located on the bus. Actual audio playback is done by SoundtrackManager and
 * game specific extensions of that class. This dummy is present in order to avoid
 * printing out warnings for writes to the APU.
 */
public class Apu implements ReadWriteRangeProvider {

    private Apu4015 apu4015 = new Apu4015();

    private Apu4017 apu4017 = new Apu4017();

    private ApuControlRegisters apuControlRegisters = new ApuControlRegisters();

    private List<ReadWriteRange> registers = new ArrayList<ReadWriteRange>();

    public Apu() {
        registers.add(apu4015);
        registers.add(apu4017);
        registers.add(apuControlRegisters);
    }

    @Override
    public List<ReadWriteRange> provideReadWriteRanges(Bus.BusType busType) {
        return registers;
    }

    private class Apu4015 implements ReadWriteRange {

        @Override
        public byte read(int address) {
            return 0;
        }

        @Override
        public void write(int address, byte value) {

        }

        @Override
        public int lower() {
            return 0x4015;
        }

        @Override
        public int upper() {
            return 0x4015;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {

        }

        @Override
        public void load(InputStream inputStream) throws IOException {

        }
    }

    private class Apu4017 implements ReadWriteRange {

        @Override
        public byte read(int address) {
            return 0;
        }

        @Override
        public void write(int address, byte value) {

        }

        @Override
        public int lower() {
            return 0x4017;
        }

        @Override
        public int upper() {
            return 0x4017;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {

        }

        @Override
        public void load(InputStream inputStream) throws IOException {

        }
    }

    private class ApuControlRegisters implements ReadWriteRange {

        @Override
        public byte read(int address) {
            return 0;
        }

        @Override
        public void write(int address, byte value) {

        }

        @Override
        public int lower() {
            return 0x4000;
        }

        @Override
        public int upper() {
            return 0x4013;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {

        }

        @Override
        public void load(InputStream inputStream) throws IOException {

        }
    }
}
