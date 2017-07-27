package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 7/27/2017.
 *
 * Nametable ram for vertical mirroring.
 * VerticalMirroringRam was added to support games which write to the
 * mirrored ranges of vram. Previously, all games reliably were writing only
 * to the ranges of the base addresses of both nametables for their respective
 * mirroring modes. This class is backwards compatible with the previous code which
 * was just one contiguous chunk of ram for $2000 and $2400. This class allocates
 * precisely the same amount of bytes as the previous approach, the only difference
 * is it maps itself over the full range of nametable addresses, and mirrors any
 * read or write past $2800 back to $2000 and $2400.
 */
public class VerticalMirroringRam implements ReadWriteRange {

    private int lower;
    private int upper;

    private byte[] ram;

    public VerticalMirroringRam() {
        int lower = Ppu.NAME_TABLE_0_BASE_ADDRESS;
        this.lower = lower;

        //Two nametables!
        int size = Ppu.NAMETABLE_RAM_SIZE * 2;

        //But we map across all four nametables on the PPU bus!
        this.upper = lower + (size * 2) - 1;

        //Over-allocate space for this chunk of ram and align its base address
        //to its location on the bus. This allows direct access without having
        //to subtract the lower range every time we do a read or write.
        this.ram = new byte[lower + size];
    }

    /**
     * Mirrors any write to the third and fourth nametable on the PPU bus back to
     * the first and second, for vertical mirroring.
     * @param address Address to mirror.
     * @return Mirrored address value.
     */
    private int mirrorAddress(int address) {
        if (address >= 0x2800)
            return address - 0x800;
        else
            return address;
    }

    @Override
    public byte read(int address) {
        return ram[mirrorAddress(address)];
    }

    @Override
    public void write(int address, byte value) {
        ram[mirrorAddress(address)] = value;
    }

    @Override
    public int lower() {
        return lower;
    }

    @Override
    public int upper() {
        return upper;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {
        outputStream.write(ram);
    }

    @Override
    public void load(InputStream inputStream) throws IOException {
        inputStream.read(ram);
    }
}
