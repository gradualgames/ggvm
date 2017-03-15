package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by derek on 8/21/2016.
 *
 * A bus provides a memory map which is nothing more than an array of
 * ReadWriteRange objects, whose references are duplicated across the
 * entire address range that those objects specify by their lower()
 * and upper() range methods. This allows reading and writing to different
 * components on the bus as easy as an array lookup.
 */
public abstract class Bus {

    public enum BusType {
        CPU,
        PPU
    }

    /**
     * The bus type: CPU or PPU. This directs ReadWriteRangeProviders to provide
     * ReadWriteRanges either for the cpu or the ppu bus depending on this bus type.
     */
    protected BusType busType;

    /**
     * This is the memory map. It is just an array of references to objects which can
     * perform read and write operations. Many of these references will be duplicates; for
     * example locations 0 through 2047 are all mapped to the Ram object.
     */
    protected ReadWriteRange[] memoryMap;

    /**
     * This object is installed throughout the memory map wherever no ram, rom or hardware
     * was installed. Any reads or writes to this object will generate a warning in the log
     * file.
     */
    protected ReadWriteRangeNop readWriteRangeNop;

    public Bus(BusType busType, int memoryMapSize, ReadWriteRangeNop readWriteRangeNop) {
        this.busType = busType;
        this.readWriteRangeNop = readWriteRangeNop;
        memoryMap = new ReadWriteRange[memoryMapSize];
    }

    /**
     * Saves out every ReadWriteRange's state to the passed in output stream.
     * This is used by GGVm as part of doing a full save-state of the vm.
     */
    public void save(OutputStream outputStream) throws IOException {
        Set<ReadWriteRange> readWriteRangeSet = new HashSet<ReadWriteRange>();
        for(int i = 0; i < memoryMap.length; i++) {
            ReadWriteRange readWriteRange = memoryMap[i];
            if (!readWriteRangeSet.contains(readWriteRange)) {
                readWriteRange.save(outputStream);
                readWriteRangeSet.add(readWriteRange);
            }
        }
    }

    /**
     * Reads every ReadWriteRange's state from the passed in input stream.
     * This is used by GGVm as part of restoring a full save-state of the vm.
     */
    public void load(InputStream inputStream) throws IOException {
        Set<ReadWriteRange> readWriteRangeSet = new HashSet<ReadWriteRange>();
        for(int i = 0; i < memoryMap.length; i++) {
            ReadWriteRange readWriteRange = memoryMap[i];
            if (!readWriteRangeSet.contains(readWriteRange)) {
                readWriteRange.load(inputStream);
                readWriteRangeSet.add(readWriteRange);
            }
        }
    }

    /**
     * Adds a ReadWriteRange object to the Bus's memory map.
     * Duplicate reference to readWriteRange for its entire address range,
     * so it can be very quickly looked up and used for any address in its
     * range.
     */
    protected void add(ReadWriteRange readWriteRange) {
        for(int i = readWriteRange.lower(); i <= readWriteRange.upper(); i++) {
            memoryMap[i] = readWriteRange;
        }
    }

    /**
     * Adds all ReadWriteRange objects provided by a ReadWriteRangeProvider.
     */
    protected void add(ReadWriteRangeProvider readWriteRangeProvider) {
        for(ReadWriteRange readWriteRange: readWriteRangeProvider.provideReadWriteRanges(busType)) {
            add(readWriteRange);
        }
    }

    /**
     * Fills all null entries in the memory map with a readWriteRange object.
     */
    protected void fillNullEntries(ReadWriteRange readWriteRange) {
        for(int i = 0; i < memoryMap.length; i++) {
            if (memoryMap[i] == null) {
                memoryMap[i] = readWriteRange;
            }
        }
    }

    /**
     * Reads a little endian word at the specified address.
     */
    public int readUnsignedWordAsInt(int address) {
        int lo = readUnsignedByteAsInt(address);
        int hi = readUnsignedByteAsInt(address + 1) << 8;
        return lo | hi;
    }

    /**
     * Read a value from the memory map, using whatever ReadWriteRange
     * object is mapped at that location. Casts to an int and strips
     * sign extension from the byte.
     */
    public int readUnsignedByteAsInt(int address) {
        return memoryMap[address].read(address) & 0xff;
    }

    /**
     * Read a signed byte from the memory map, using whatever ReadWriteRange
     * object is mapped at that location.
     */
    public byte readSignedByte(int address) {
        return memoryMap[address].read(address);
    }

    /**
     * Writes value to the memory map, using whatever ReadWriteRange
     * object is mapped at that location. Casts the passed in value to
     * a byte.
     */
    public void writeIntAsByte(int address, int value) {
        memoryMap[address].write(address, (byte) value);
    }

    /**
     * Installs a bus event generator to a specific address range on the bus. Bus
     * event generators forward all read and write calls to whatever they are replacing on
     * the bus, but also fire an event back to the BusListener passed in here that a read
     * or write has occurred.
     */
    public void installBusEventGenerator(int address, int size, BusListener busListener) {
        BusEventGenerator busEventGenerator = new BusEventGenerator(address, size, memoryMap[address], busListener);
        for(int i = address; i < address + size; i++) {
            memoryMap[i] = busEventGenerator;
        }
    }

    /**
     * Uninstalls bus event generator references from the bus, replacing them with
     * the ReadWriteRange reference stored in the bus event generator.
     */
    public void uninstallBusEventGenerator(int address, int size) {
        for(int i = address; i < address + size; i++) {
            if (memoryMap[i] instanceof BusEventGenerator) {
                BusEventGenerator busEventGenerator = (BusEventGenerator) memoryMap[i];
                memoryMap[i] = busEventGenerator.getReadWriteRange();
            }
        }
    }
}
