# GGVm Guide
### by Derek Andrews <gradualgames@gmail.com>
***

GGVm is a porting layer for NES homebrew games. It's similar to
an emulator, but a small amount of customization needs to be done
for most games. The goal is for it to be a convenient, free,
no-strings attached way for an NES homebrew developer to get
their game wrapped up and ready to publish on multiple platforms
without having to pay anybody or ask for permission.

GGVm supports a subset of the NES's full capabilities. This
subset was chosen to support the needs of most of today's
homebrew developers. This enabled us to complete the project
quickly enough to return to creating homebrew games. In
addition, this allowed us to take large shortcuts in how the
system is emulated, which enables games to run smoothly at 60
fps with little or no stuttering even on somewhat old Android
devices. Please read the feature list below to get an idea of
what GGVm's capabilities are.

# Project goals
***

- Port - Provide as little of the NES hardware as possible for as
much performance on as many devices as possible, requiring some
work on the part of the developer.

- Package - Your game can be distributed as a Jar, a Jar+JRE, an
APK, or an IPA.

- Protect - You can omit the iNES header, and remove your sound
engine implementation from your game ROM (your soundtrack will
be played as ogg, mp3 or wav files) Any casual hackers who
successfully fill in your iNES header and run your game in an
emulator will have wasted their time---no sound.

# License

The license is in LICENSE in the root directory and is
The Unlicense. See unlicense.org.

There is a second license, license.txt, in assets/dushlan which
is the BSD license, and applies only to the Dushlan binary and
assets.

# Credits
***

* Derek Andrews - Author of GGVm

* Justin Mullin - For telling me about LibGDX and helping me come up with a way to imitate a palettized graphics mode using a fragment shader, as well as advice about performance.

* LibGDX (https://libgdx.badlogicgames.com/) - The cross plat-form game Java game development library used to develop GGVm.

* Peter McQuillan - Creator of Dúshlán, an open source game for the NES, used to demonstrate GGVm. See https://github.com/soiaf/Dushlan for more information. BSD license for his binary included in assets/dushlan/license.txt.

* Brad Smith (rainwarrior) - For inspiring me to come up with a way to get my games on Steam.

* Greg Caldwell - For inspiring me to come up with a way to get my games on Steam.

* Adam Welch (dra600n) - For being the first guinea pig to use
GGVm for a digitally distributed NES game besides our own
titles.

* Rob Bryant (Roth) - For helping with Adam Welch's Get 'em Gary
GGVm release.

* Joe Granato - For also inspiring me to think big and get my
games out there, for extensive use of GGVm which prodded me to
make many improvements, and finally for free publicity for
GGVm (as well as other Gradual Games' efforts).

* Kevin Hanley (KHAN Games) - For testing an iOS build of his
game, The Incident, on his iPad.

* Alex Semenov (Shiru) - For bug reports and pointing Joe Granato
to GGVm. Also for inspiring me to work really hard on
performance issues on mobile devices.

# Changes
***

* 3-12-17: Created first version of GGVM README.txt file.

# Dependencies
***

- Java Runtime:
You will need this to use GGVm's build system, Gradle.

- Java Development Kit:
I have been building with JDK 1.8 with no issues, but you should
be able to use a JDK as old as 1.6 with LibGDX.

- Android SDK:
You will need this to build for Android.

- XCode:
You will need this to build for iOS.

# Features and Limitations
***

- Smooth 60fps play on pc systems up to ~8 years old and on
Android phones and tablets up to ~3 years old that I have
tested. As of this writing, iOS confirmed working well.

- No input lag added beyond latency already present in the
controller subsystem.

- Games must not rely on CPU for timing, only nmi (for example,
 Dushlan had used cpu for gameplay timing---I modified it to
 use nmis instead)

- 6502 cpu core. Only write your game ONCE, for the NES!

- No real PPU emulation. Adapters must be coded for raster
 effects, per-game. (a couple hours' work)

- No real APU emulation. Adapters must be coded per-game, which
 play OGG files when sound engine routines are called. (a couple hours' work)

- Background sprites may partially hide foreground sprites,
 assuming bg sprites are solid 8x8 squares of pixels.

- You can leave out the iNES header and configure from GGVM,
which discourages someone from extracting your rom.

- Due to coding audio adapter, you can gut sound-engine. Thus
 if anyone extracts your ROM, it'll have no audio and they
 wasted their time.

- Mapper 0 and 2 supported, 4 coming soon

- CHR-RAM supported during transitions. Live updates during
 gameplay are not supported.

- No undocumented cpu opcodes supported yet, but can add support

- Automatic save/restore state

- Controller configuration for XBOX 360 and Retrousb controllers,
 manual config for others

- Windowed or fullscreen mode

# Supported platforms
***

Games can be packaged up for the following platforms:

- Windows
- Mac
- Linux
- Android
- iOS

LibGDX is also capable of using GWT to compile and run in a
web browser. I have not tested GGVm with this and I have no
personal interest in distributing for web. But if anybody
expresses interest maybe I'll try to get that up and running
eventually.

# Setting up local.properties
***

The local.properties file is used to configure the build for all
platforms. local.properties.example has been provided for you to
give examples of what the various variables need to be set to.
The GGVm snapshot contains a rom and assets for Dushlan, an open
source NES game, to demo GGVm. Without a game and associated
java classes, GGVm will fail to build.

# GGVm's build system
***

GGVm uses Gradle, a self-bootstrapping build system which uses
Groovy. On Windows based machines, you can invoke all build
tasks by typing

gradlew.bat

followed by the project name, a colon, and a task name. Examples
will be given in platform specific build sections of this
document.

On *nix based machines, you can invoke all build tasks by typing

./gradlew

Note that you may have to chmod +x to the gradlew script on some
machines after unzipping.

For the rest of this document, for brevity I will be typing the
*nix style of invocation, so please keep this in mind if you are
on a Windows machine.

# Naming and case sensitivity
***

Note that GGVm's build system is case sensitive. Your assets
directory must be all lowercase. But all other instances of your
game name must exactly match what is in local.properties. In
other words, if your game is Dushlan, then each game specific
class that Dushlan needs MUST have the exact string Dushlan in
the name, or your build will fail. Please keep this in mind
through the rest of this document.

# The assets directory
***

The assets directory is where you place subdirectories for each
game you want to wrap up with ggvm, each which contain the game
rom, game-specific Java classes, and icons for display on the PC
executable, Android launcher and window. Depending on which
features you are using for your game, some of these directories
are optional. A description follows. Note the dushlan directory
is used as an example.

dushlan/icon:

Optional. Only if you're using distPackrIcon or if you want to
include an icon with an Android or iOS build.

dushlan/music:

Not optional, assuming your game has music.

dushlan/sfx:

Not optional, assuming your game has sound effects.

dushlan/src/com/gradualgames/manager/nmi:

Depending on how robust your game is against nmi landing in
unsafe spots, you may need an NmiSafeFunctor class to protect
against this. In many cases, this can be written as an anonymous
inner class right inside your game's module. See later sections
for more information.

dushlan/src/com/gradualgames/manager/rastereffect:

Optional. If your game requires special behavior to modify the
screen after normal rendering, raster effect managers will go
here. Note that depending on your game, more extensive modifications
to GGVm may be required to support your chosen raster effect.

dushlan/src/com/gradualgames/manager/soundtrack/DushlanSoundtrackManager.java:

Not optional assuming your game has music. This class is required
to respond to your game executing sound engine routines so that
it can map the parameters to these routines to sound file
playback.

dushlan/src/com/gradualgames/module/DushlanGameModule.java

Absolutely not optional. This is the only file besides the rom
that you absolutely MUST have in your assets directory to build
your game. It fully configures GGVm with all dependencies
required to run your game.

# Building a game for PC
***

To run the currently configured game (see section about the
local.properties file) on PC, type:

./gradlew desktop:run

To package up the currently configured game in a jar, type:

./gradlew desktop:dist

To package up the currently configured game with a JRE for
distribution, type:

./gradlew desktop:distPackr

Note that using this task requires the packr utility, and a
JDK zip file to be present on the file system and configured
using local.properties.

To package up the currently configured game with a JRE AND
modify the resulting exe to include an icon, type:

./gradlew desktop:distPackrIcon

The icon will be obtained from the game's assets directory under
a folder called icon, and it will look for a file called
icon.ico. Note that the demo game, Dushlan, currently does not
have an icon included. Note also that this task requires
resourcehacker.exe to be installed, and only works on Windows
machines at the moment.

distPackrIcon is what typically will be used for a full Windows
PC release, say for Steam.

# Building a game for Android
***

To run the currently configured game (see section about the
local.properties file) on Android, type:

./gradlew android:run

To build the currently configured game as an APK for Android,
type:

./gradlew android:assembleDebug

NOTE: Future versions of this README.txt will have information
on how to build a release version of an Android APK. If you are
familiar with Android already, you might not have much trouble
getting this to work without help from this file.

# Building a game for iOS
***

NOTE: As of the first writing of this document, iOS builds are
highly experimental. I have seen them run in a simulator, and
KHAN Games has seen it run on an iPad. I am currently in the
process of getting an App Store license so that I can firm up
the iOS build. Then I will be updating this section with
instructions.

# Instructions for creating a custom GameModule
***

Any game you build with GGVm MUST have its own extension of the
GameModule class. For example, Dushlan has the following file:

dushlan/src/com/gradualgames/module/DushlanGameModule.java

To create your own game's GameModule class, it MUST have the
exact name as the game variable in local.properties, followed
by GameModule, or the build will fail. Please see the Dushlan
example to get an understanding of what all must be provided
by the GameModule class. Most of it should be fairly self-
explanatory. Some things to note however:

-provideCartridge can provide a Cartridge either specifying the
rom configuration totally manually, or just allow it to
interpret the iNES file for you. It is recommended to strip the
iNES header from your ROM and configure from this method
instead, to help discourage casual hackers from pulling your rom
out of the distributed game and using it in an emulator. Here is
an example:

```
    @Override
    public Cartridge provideCartridge(byte[] bytes) {
        return new Cartridge(32, 0, 2, Cartridge.MIRRORING_MODE_VERTICAL, bytes);
    }
```

This configures the cartridge for 32 prg roms, 0 chr roms,
mapper 2, and vertical mirroring. Thus, the iNES header will be
ignored and can be removed from the ROM.

# An example, totally blank GameModule.
***

To help you get your own game running in GGVm, here is a
complete, but totally blank GameModule class. Note that this
class MUST be placed at:
assets/mytitle/src/com/gradualgames/module/MyTitleGameModule.java

Note also "MyTitle" can and should be replaced with the name of
your game anywhere it is found, preserving case.

If your game's iNES configuration is currently supported by
GGVm, and you are returning the correct vertical or horizontal
mirroring rendering manager (see below), your game should run,
without sound. See above sections for instructions on building
and running your game on your desired platform.

```
package com.gradualgames.module;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.Cartridge;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.manager.nmi.NmiSafeFunctor;
import com.gradualgames.manager.rastereffect.RasterEffectManager;
import com.gradualgames.manager.render.RenderManager;
import com.gradualgames.manager.render.VerticalMirroringRenderManager;
import com.gradualgames.manager.soundtrack.SoundtrackManager;

/**
 * Created by derek on 1/17/2017.
 */
public class MyTitleGameModule implements GameModule {
    @Override
    public String provideTitle() {
        return "MyTitle";
    }

    @Override
    public String provideFileName() {
        return "mytitle/mytitle.nes";
    }

    @Override
    public String provideFontBitmapFileName() {
        return "font/gameover.png";
    }

    @Override
    public String provideFontFileName() {
        return "font/gameover.fnt";
    }

    @Override
    public String provideIconFileName() {
        return null;
    }

    @Override
    public Cartridge provideCartridge(byte[] bytes) {
        return new Cartridge(bytes);
    }

    @Override
    public RenderManager provideRenderManager(GGVm ggvm, RasterEffectManager rasterEffectManager) {
        return new VerticalMirroringRenderManager(ggvm, rasterEffectManager);
    }

    @Override
    public RasterEffectManager provideRasterEffectManager(GGVm ggvm) {
        return new RasterEffectManager() {
            @Override
            public void render(SpriteBatch spriteBatch) {

            }
        };
    }

    @Override
    public SoundtrackManager provideSoundtrackManager(GGVm ggvm) {
        return new SoundtrackManager("mytitle", ggvm) {
            @Override
            protected void handleOnRead(int address) {

            }

            @Override
            protected void handleOnWrite(int address, byte value) {

            }
        };
    }

    @Override
    public NmiSafeFunctor provideNmiSafeFunctor() {
        return new NmiSafeFunctor() {
            @Override
            public boolean isPcInSafeRange(int pc) {
                return true;
            }
        };
    }
}
```
# Instructions for creating a custom SoundtrackManager
***

A gamemodule must at least provide an anonymous inner class
with a no-op SoundtrackManager extension, as shown in the
above section. However, if you want music and sound to play back
in your game, you need to implement the SoundtrackManager.
Dushlan has a SoundtrackManager you can look at for an example
of what you will need to do.

dushlan/src/com/gradualgames/manager/soundtrack/DushlanSoundtrackManager.java

A SoundtrackManager listens to several key locations in a game's
rom. These locations typically will be sound engine routines,
such as routines which play music, sfx, stop, pause, or other
functionality. The constructor of the SoundtrackManager will
install listeners to these locations, something like this from
DushlanSoundtrackManager.java:

ggvm.installBusEventGenerator(play_song, 1, this);

This says that any time the address play_song (defined as an int
elsewhere in the class) is executed, call into this soundtrack
manager with that address.

All the callbacks from these listeners will go into two methods,
handleOnRead and handleOnWrite, which are abstract in the
SoundtrackManager.java base class, forcing you to override both.

Typically you will only need to work with handleOnRead, but
there may be special scenarios in some games which require you
to listen to a write somewhere that controls the sound playback.

Most handleOnRead implementations will look something like what
you see in DushlanSoundtrackManager.java. It will just be a
switch case which first inspects the address to see which rout-
ine was called. Next, the soundtrack manager inspects the ram,
registers, or even currently swapped bank (for currently
supported mappers) to determine which song file to play back.

There are several methods in the base class of
SoundtrackManager.java for sound playback. The main ones you
will be using are playSong and playSfx. playSong optionally
allows you to play a song once or loop it. Depending on the
game, you may need to implement additional logic to support
pause and resume (different games accomplish this in different
ways). Songs which have an intro and looping portion also need
special treatment. An example of this special treatment is in
DushlanSoundtrackManager.java. When an intro is played, a
listener is installed on nmi to poll the current song for when
it is completed---and then the looping portion of the song,
initialized when the intro was played, is started and the nmi
listener removed.

Why are we doing things this way with GGVm? A couple of reasons.
One is we did not want to bother emulating the APU, and another
is we wanted as much performance as possible, so the only thing
that is cpu intensive at all is the 6502 cpu simulation, which
turns out not to be all that time-consuming.

A benefit to forcing the developer to write a soundtrack manager
is, you can remove everything but rts from your game ROM's
sound engine before publishing. This means anybody who tries to
pull your ROM out of your jar or apk and succeeds will have a
broken rom with no sound, and they wasted their time.

Another benefit is additional mixing. In a real NES game, sound
effects typically cancel their corresponding apu channel. In
GGVm, the sound effect will mix along with the music.

A downside is the degree of control you have over audio play-
back. GGVm supports most typical scenarios in use for NES
homebrew games, but a highly advanced sound system may be
difficult or impossible to fully replicate.

# Instructions for creating a custom RasterEffectManager
***

This is a largely experimental area of GGVm. The original idea
of RasterEffectManager was to be able to support scanline based
effects of the Ppu, since GGVm does not emulate the Ppu scanline
per scanline. It is in use in my own game, The Legends of Owlia,
by drawing a single black rectangle at the top of the screen
which is 16 scanline high. In the actual NES game, this was
done using precise vblank timing and empty cpu spin loops before
turning graphics on. In GGVm, I can replicate this behavior just
by drawing a black rectangle at the top of the screen to hide
scrolling updates.

For real raster effects, such as split screens, it is likely
that you will need to extend HorizontalMirroringRenderManager
or VerticalMirroringRenderManager, and then inspect cpu
registers and ram similar to how SoundtrackManager works to find
out where split points are, and then render the nametable in
a top and a bottom section. It is also likely you will need to
use fbos to do this. I'm planning to do something like this for
my current game, so I will be diving into this at some point in
the future. I'm also open to contributions if anybody wants to
dive in to this.

# Instructions for creating an NmiSafeFunctor
***

These are optional, but often are very helpful for making sure
a game is stable in GGVm. Since GGVm makes no attempt whatsoever
at being an accurate emulator, this means that the likelihood
that nmi will fire in unsafe spots in your code will be a lot
higher. Many NES homebrewers are content to "tune" their code so
that these situations do not happen, and rely on the precise
timing of the real hardware or emulators to avoid garbage tiles
or other related crashes. GGVm cannot provide precise timing, so
enter NmiSafeFunctor.

It is just a class which has a function that checks whether the
program counter is within a safe range. For most games, safe
ranges will be standard nmi wait loops. Typically nmi wait loops
involve a counter that is incremented from nmi, and then a loop
in the main loop of the game that looks something like this:

```
    lda nmi_counter
:
    cmp nmi_counter
    beq :-
```

The NmiSafeFunctor must check to see whether the program counter
is inside one of these wait loops. This is always used right
before firing nmi, and nmi will not fire if the program counter
is not within a safe range. If your game has these repeated
throughout the codebase, it is recommended to push these into a
routine so that your NmiSafeFunctor does not need to be very
long. You can usually get a listing from your assembler to help
you find the address range that these loops occupy. Then your
NmiSafeFunctor might look something like this (for a game that
has two such loops):
```
    @Override
    public NmiSafeFunctor provideNmiSafeFunctor() {
        return new NmiSafeFunctor() {
            @Override
            public boolean isPcInSafeRange(int pc) {
                return (pc >= 0xc080 && pc <= 0xc082) || (pc >= 0xC378 && pc <= 0xC37A);
            }
        };
    }
```
See? Not too bad. And now a whole class of nmi related bugs your
game may have exhibited in ggvm cannot happen.
