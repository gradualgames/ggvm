package com.gradualgames.ggvm;

import com.badlogic.gdx.Gdx;
import com.gradualgames.manager.nmi.NmiSafeFunctor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 8/21/2016.
 *
 * This is the entry point of the Gradual Games Virtual Machine. The user is responsible
 * for injecting a Cartridge into the GGVm, whose contents need to be loaded at a higher
 * level, integrated with the game framework. Then GGVm creates all dependencies such as
 * the cpu, the ppu, the controller, the apu, the sprite ram, the cpu and ppu bus,
 * wires them all together and resets the Cpu. Finally GGVm generates an event to the
 * application saying when to generate graphics based on chr data, and provides interface
 * methods for easily gathering information about background and sprite graphics data, the
 * palette, the controller and cpu registers.
 *
 * GGVm advances the cpu by an arbitrary number of instructions, every frame, called from
 * the game framework application object. This is usually roughly 9000 or so, which is an
 * approximation of how many instructions are executed per frame on a real NES. On every
 * frame, the game framework code is expected to call nmi. In this fashion, GGVm simulates
 * how the NES hardware behaves, at least at a very high level. Any effects the NES
 * hardware is normally capable of have to be implemented at a higher level by game
 * framework code. The intent of GGVm is to be as simple as possible and provide as much
 * performance as possible, written in Java, so it is easy to port to numerous operating
 * systems for distributing an NES game.
 */
public class GGVm implements BusListener {

    private static final int INSTRUCTIONS_PER_SECOND_LOGGING_INTERVAL = 200;

    private Cartridge cartridge;

    private PpuBus ppuBus;

    private Ppu ppu;

    private Ram cpuRam;

    private Controller controller;

    private Apu apu;

    private SpriteRam spriteRam;

    private CpuBus cpuBus;

    private Cpu cpu;

    private ReadWriteRangeNop readWriteRangeNopCpu;

    private ReadWriteRangeNop readWriteRangeNopPpu;

    private OnGeneratePatternTableListener onGeneratePatternTableListener;

    private NmiSafeFunctor nmiSafeFunctor;

    private boolean alive = false;

    private int instructionsPerSecondLoggingIntervalCounter = INSTRUCTIONS_PER_SECOND_LOGGING_INTERVAL;

    public GGVm(Cartridge cartridge, NmiSafeFunctor nmiSafeFunctor, OnGeneratePatternTableListener onGeneratePatternTableListener) {
        this.cartridge = cartridge;
        this.nmiSafeFunctor = nmiSafeFunctor;
        this.onGeneratePatternTableListener = onGeneratePatternTableListener;

        //Configure mapper based on the cartridge data.
        ReadWriteRangeProvider mapper = cartridge.configureMapper();

        //No-op objects to put on the cpu and ppu bus for warnings
        readWriteRangeNopCpu = new ReadWriteRangeNop();
        readWriteRangeNopPpu = new ReadWriteRangeNop();

        //Configure ppu and dependencies
        ppuBus = new PpuBus(mapper, readWriteRangeNopPpu);
        ppu = new Ppu(ppuBus);
        //Listen for writes to CHR-RAM
        ppuBus.installBusEventGenerator(0, 0x2000, this);

        //Configure cpu and dependencies
        cpuRam = new Ram(0, Cpu.RAM_SIZE);
        controller = new Controller();
        apu = new Apu();
        spriteRam = new SpriteRam();
        cpuBus = new CpuBus(cpuRam, controller, spriteRam, ppu, apu, mapper, readWriteRangeNopCpu);
        cpu = new Cpu(cpuBus);

        //Configure warning generators
        readWriteRangeNopCpu.setCpu(cpu);
        readWriteRangeNopCpu.setBus(cpuBus);
        readWriteRangeNopPpu.setCpu(cpu);
        readWriteRangeNopPpu.setBus(ppuBus);

        cpu.reset();
    }

    public void saveState(OutputStream outputStream) throws IOException {
        cpu.save(outputStream);
        cpuBus.save(outputStream);
        ppuBus.save(outputStream);
    }

    public void loadState(InputStream inputStream) throws IOException {
        cpu.load(inputStream);
        cpuBus.load(inputStream);
        ppuBus.load(inputStream);
    }

    public void logInstructionsPerSecond() {
        instructionsPerSecondLoggingIntervalCounter--;
        if (instructionsPerSecondLoggingIntervalCounter <= 0) {
            instructionsPerSecondLoggingIntervalCounter = INSTRUCTIONS_PER_SECOND_LOGGING_INTERVAL;
            Gdx.app.log(getClass().getSimpleName(), "Cpu is performing at: " + cpu.instructionsPerSecond() + " instructions per second.");
        }
    }

    public void printRegisters() {
        cpu.printRegisters();
    }

    public boolean isAlive() {
        return alive;
    }

    public void start() {
        stop();
        cpu.startTimer();
        alive = true;
    }

    public void stop() {
        if (alive) {
            alive = false;
            controller.clear();
        }
    }

    /**
     * Stops cpu thread, resets the cpu to the reset vector, then
     * restarts the thread. Essentially a full system reset.
     */
    public void reset() {
        stop();
        cpu.reset();
        start();
    }

    public void nmi() {
        if (alive) {
            ppu.setInVblank();
            if (ppu.isNmiEnabled()) {
                if (nmiSafeFunctor.isPcInSafeRange(cpu.getPc())) {
                    cpu.nmi();
                }
            }
        }
    }

    /**
     * Installs a BusEventGenerator on the cpu bus. This can be used to
     * infer that a song is about to be played, and turn around and play the
     * corresponding asset at a higher level. This allows us to avoid emulating
     * the APU! :D
     * @param address
     * @param size
     * @param busListener
     */
    public void installBusEventGenerator(int address, int size, BusListener busListener) {
        cpuBus.installBusEventGenerator(address, size, busListener);
    }

    /**
     * Removes BusEventGenerator references from the bus.
     * @param address
     * @param size
     */
    public void uninstallBusEventGenerator(int address, int size) {
        cpuBus.uninstallBusEventGenerator(address, size);
    }

    /**
     * Return current value of cpu accumulator.
     * @return Register value
     */
    public int getA() {
        return cpu.getA();
    }

    /**
     * Return current value of cpu x register.
     * @return Register value
     */
    public int getX() {
        return cpu.getX();
    }

    /**
     * Return current value of cpu y register.
     * @return Register value
     */
    public int getY() {
        return cpu.getY();
    }

    /**
     * Reads a byte from the cpu bus stripping sign information.
     * @param address
     * @return
     */
    public int readUnsignedByteAsInt(int address) {
        return cpuBus.readUnsignedByteAsInt(address);
    }

    /**
     * Reads a word from the cpu bus.
     * @param address
     * @return
     */
    public int readWord(int address) {
        return cpuBus.readUnsignedWordAsInt(address);
    }

    /**
     * Returns current lower prg bank, if the current game uses a mapper which
     * supports this.
     * @return
     */
    public int getLowerPrgBank() {
        if (cpuBus.memoryMap[0x8000] instanceof RomSwitchboard) {
            RomSwitchboard romSwitchboard = (RomSwitchboard) cpuBus.memoryMap[0x8000];
            return romSwitchboard.getCurrentRom();
        }
        return -1;
    }

    /**
     * Sets the specified button to pressed or unpressed state.
     * @param buttonIndex The button index. See constants in Controller
     * @param pressed Whether or not the buttton is pressed.
     */
    public void setButtonState(int buttonIndex, boolean pressed) {
        controller.getButtons()[buttonIndex] = (byte) (pressed ? 1: 0);
    }

    /**
     * Sets the A button state.
     * @param pressed
     */
    public void setAButtonState(boolean pressed) {
        controller.getButtons()[Controller.Buttons.A.ordinal()] = (byte) (pressed ? 1: 0);
    }

    /**
     * Sets the B button state.
     * @param pressed
     */
    public void setBButtonState(boolean pressed) {
        controller.getButtons()[Controller.Buttons.B.ordinal()] = (byte) (pressed ? 1: 0);
    }

    /**
     * Sets the select button state.
     * @param pressed
     */
    public void setSelectButtonState(boolean pressed) {
        controller.getButtons()[Controller.Buttons.SELECT.ordinal()] = (byte) (pressed ? 1: 0);
    }

    /**
     * Sets the start button state.
     * @param pressed
     */
    public void setStartButtonState(boolean pressed) {
        controller.getButtons()[Controller.Buttons.START.ordinal()] = (byte) (pressed ? 1: 0);
    }

    /**
     * Sets the up button state.
     * @param pressed
     */
    public void setUpButtonState(boolean pressed) {
        controller.getButtons()[Controller.Buttons.UP.ordinal()] = (byte) (pressed ? 1: 0);
    }

    /**
     * Sets the down button state.
     * @param pressed
     */
    public void setDownButtonState(boolean pressed) {
        controller.getButtons()[Controller.Buttons.DOWN.ordinal()] = (byte) (pressed ? 1: 0);
    }

    /**
     * Sets the left button state.
     * @param pressed
     */
    public void setLeftButtonState(boolean pressed) {
        controller.getButtons()[Controller.Buttons.LEFT.ordinal()] = (byte) (pressed ? 1: 0);
    }

    /**
     * Sets the right button state.
     * @param pressed
     */
    public void setRightButtonState(boolean pressed) {
        controller.getButtons()[Controller.Buttons.RIGHT.ordinal()] = (byte) (pressed ? 1: 0);
    }

    /**
     * Retrieves whether the background is enabled.
     * @return
     */
    public boolean isBackgroundVisible() {
        return ppu.isBackgroundVisible();
    }

    /**
     * Retrieves whether the display type is currently monochrome.
     * @return
     */
    public boolean isMonochromeDisplayType() { return ppu.isMonochromeDisplayType(); }

    /**
     * Retrieves which pattern table to be used for generating background graphics.
     * @return 0 for $0000 or 1 for $1000
     */
    public int getBackgroundPatternTableAddress() {
        return ppu.getBackgroundPatternTableAddress();
    }

    /**
     * Retrieves which pattern table to be used for generating sprite graphics.
     * @return 0 for $0000 or 1 for $1000
     */
    public int getSpritePatternTableAddress() {
        return ppu.getSpritePatternTableAddress();
    }

    /**
     * Retrieves a 16 byte palette from the ppuBus
     * @param spritePalette Whether or not to retrieve the bg or sprite palette.
     * @return The palette
     */
    public int[] getPalette(boolean spritePalette) {
        int paletteBaseAddress = spritePalette ? Ppu.SPR_PALETTE_BASE_ADDRESS : Ppu.BG_PALETTE_BASE_ADDRESS;
        int[] palette = new int[16];
        if (isMonochromeDisplayType()) {
            for (int i = 0; i < 16; i++) {
                palette[i] = ppuBus.readUnsignedByteAsInt(paletteBaseAddress + i) & 0xf0;
            }
        } else {
            for (int i = 0; i < 16; i++) {
                palette[i] = ppuBus.readUnsignedByteAsInt(paletteBaseAddress + i);
            }
        }
        return palette;
    }

    /**
     * Retrieves a pixel, whose value will be 0 to 4, from the given chr tile, on the
     * Ppu bus. This aids framework code in knowing less about how the NES works so it
     * can focus purely on generating graphics.
     * @param tileIndex
     * @param x
     * @param y
     * @return
     */
    public int getChrPixel(int tileIndex, int x, int y) {
        int tileRamAddress = tileIndex * 16;
        int chrRamLoBitByte = ppuBus.readUnsignedByteAsInt(tileRamAddress + y);
        int chrRamHiBitByte = ppuBus.readUnsignedByteAsInt(tileRamAddress + y + 8);
        int loBit = (chrRamLoBitByte & (1 << x)) >> x;
        int hiBit = ((chrRamHiBitByte & (1 << x)) >> x) << 1;
        int pixel = hiBit | loBit;
        return pixel;
    }

    /**
     * Retrives a nametable tile from the given name table at x and y in
     * nametable units.
     * @param nameTableAddress
     * @param x
     * @param y
     * @return
     */
    public int getNametableTile(int nameTableAddress, int x, int y) {
        int nametableX = x;
        int nametableY = y;
        return ppuBus.readUnsignedByteAsInt(nameTableAddress + (nametableY * 32) + nametableX);
    }

    /**
     * Retrieves the attribute for a given nametable tile index where x and y are
     * in nametable units (0-31, 0-29)
     * @param attributeTableAddress
     * @param x X coordinate of nametable tile attribute to retrieve
     * @param y Y coordinate of nametable tile attribute to retrieve
     * @return The attribute applied to the nametable tile at this location.
     */
    public int getAttributeForNametableTile(int attributeTableAddress, int x, int y) {
        int attributeX = x >> 2;
        int attributeY = y >> 2;
        int subAttributeX = (x >> 1) & 1;
        int subAttributeY = (y >> 1) & 1;
        int attributeByte = ppuBus.readUnsignedByteAsInt(attributeTableAddress + attributeY * 8 + attributeX);
        if (subAttributeY == 1) {
            attributeByte >>= 4;
        }
        if (subAttributeX == 1) {
            attributeByte >>= 2;
        }
        int attribute = (attributeByte & 0x3);
        return attribute;
    }

    /**
     * Retrieves current X scroll from ppu.
     * @return
     */
    public int getScrollX() {
        return ppu.getScrollX();
    }

    /**
     * Retrieves current Y scroll from ppu.
     * @return
     */
    public int getScrollY() {
        return ppu.getScrollY();
    }

    /**
     * Retrieves current vram address.
     * @return
     */
    public int getNametableAddress() {
        return ppu.getNametableAddress();
    }

    /**
     * Returns sprite size.
     * @return Returns 0 for 8x8 and 1 for 8x16
     */
    public int getSpriteSize() { return ppu.getSpriteSize(); }

    /**
     * Retrives sprite X coordinate for the given sprite index, 0-63
     * @param spriteIndex
     * @return
     */
    public int getSpriteX(int spriteIndex) {
        return spriteRam.readUnsignedByteAsInt((spriteIndex << 2) + 3);
    }

    /**
     * Retrives sprite Y coordinate for the given sprite index, 0-63
     * @param spriteIndex
     * @return
     */
    public int getSpriteY(int spriteIndex) {
        return spriteRam.readUnsignedByteAsInt((spriteIndex << 2));
    }

    /**
     * Retrives sprite tile for the given sprite index, 0-63
     * @param spriteIndex
     * @return
     */
    public int getSpriteTile(int spriteIndex) {
        return spriteRam.readUnsignedByteAsInt((spriteIndex << 2) + 1);
    }

    /**
     * Retrives sprite color attribute for the given sprite index, 0-63
     * @param spriteIndex
     * @return
     */
    public int getSpriteColorAttribute(int spriteIndex) {
        return (spriteRam.readUnsignedByteAsInt((spriteIndex << 2) + 2) & 3);
    }

    /**
     * Retrieves the sprite behind background flag.
     * @param spriteIndex
     * @return
     */
    public boolean getSpriteIsBehindBackground(int spriteIndex) {
        return (spriteRam.readUnsignedByteAsInt((spriteIndex << 2) + 2) & 0x20) != 0;
    }

    /**
     * Retrieves the sprite horizontal flip flag.
     * @param spriteIndex
     * @return
     */
    public boolean getSpriteHorizontalFlip(int spriteIndex) {
        return (spriteRam.readUnsignedByteAsInt((spriteIndex << 2) + 2) & 0x40) != 0;
    }

    /**
     * Retrieves the sprite vertical flip flag.
     * @param spriteIndex
     * @return
     */
    public boolean getSpriteVerticalFlip(int spriteIndex) {
        return (spriteRam.readUnsignedByteAsInt((spriteIndex << 2) + 2) & 0x80) != 0;
    }

    /**
     * Advances the cpu by instructionCount instructions.
     * @param instructionCount Number of instructions to execute.
     */
    public void advance(int instructionCount) {
        while(instructionCount-- >= 0) {
            cpu.execute();
        }
    }

    @Override
    public void onRead(int address) {

    }

    @Override
    public void onWrite(int address, byte value) {
        //If we're writing the very last byte of a chr tile
        if ((address & 0xf) == 0xf) {
            onGeneratePatternTableListener.onGeneratePattern(address - 0xf);
        }
    }
}
