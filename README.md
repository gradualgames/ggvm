# GGVm Guide
### by Derek Andrews <gradualgames@gmail.com>

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
devices. Please read the feature list to get an idea of
what GGVm's capabilities are.

# Project Mission Statement

Please use this mission statement as a guide if you would like
to contribute to the project.

- Port - Provide as little of the NES hardware as possible for as
much performance on as many devices as possible, requiring some
work on the part of the developer.

- Package - Your game can be distributed as a Jar, a Jar+JRE, an
APK, or an IPA.

- Protect - You can obfuscate the iNES header, and remove your sound engine implementation from
your game ROM (your soundtrack will be played as ogg, mp3 or wav files)
Any casual hackers who successfully fill in your iNES header and run
your game in an emulator will have wasted their time: no sound. If
you are using any raster effects, these, too, can be omitted from the
rom and adapted by customizing GGVm instead.

# Features and Limitations

- Smooth 60fps play on pc systems up to ~8 years old and on
Android phones and tablets up to ~3 years old that I have
tested. iOS working just as well.

- No input lag added beyond latency already present in the
controller subsystem.

- Games must not rely on CPU for timing, only nmi (for example,
 Dushlan had used cpu for gameplay timing. I modified it to
 use nmis instead)

- 6502 cpu core. Only write your game ONCE, for the NES!

- No real PPU emulation. Split screen capabilities are provided via
special registers. See [Virtual Registers Reference](#virtual-registers-reference).

- No real APU emulation. Games must write to special GGVm only
hardware registers to play music and sfx back from a list that you
provide from your game module. See [An Example GameModule](#an-example-gamemodule) and [Virtual Registers Reference](#virtual-registers-reference).

- Background sprites may partially hide foreground sprites,
 assuming bg sprites are solid 8x8 squares of pixels.

- You can obfuscate the iNES header and configure from GGVM,
which discourages someone from extracting your rom.

- Due to using [Audio Playback Registers](#audio-playback-registers), you can gut your sound engine. Thus
 if anyone extracts your ROM, it'll have no audio and they
 wasted their time.

- Mapper 0 and 2 supported, 4 coming soon

- Live CHR-RAM streaming supported

- No undocumented cpu opcodes supported yet, but can add support

- Automatic save/restore state

- Controller configuration for XBOX 360 and Retrousb controllers,
 manual config for others

- Windowed or fullscreen mode

# Supported Platforms

Games can be packaged up for the following platforms:

- Windows
- Mac
- Linux
- Android
- iOS

# License

The license is in LICENSE in the root directory and is
The Unlicense. See unlicense.org.

There is a second license, license.txt, in assets/dushlan which
is the BSD license, and applies only to the Dushlan binary and
assets.

# Credits

* Derek Andrews - Author of GGVm

* Justin Mullin - For telling me about LibGDX and helping me come up with a way to imitate a palettized graphics mode using a fragment shader, as well as advice about performance.

* LibGDX (https://libgdx.badlogicgames.com/) - The cross platform game Java game development library used to develop GGVm.

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

# Dependencies

- Java Runtime:
You will need this to use GGVm's build system, Gradle.

- Java Development Kit:
I have been building with JDK 1.8 with no issues, but you should
be able to use a JDK as old as 1.6 with LibGDX.

- Android SDK:
You will need this to build for Android.

- XCode:
You will need this to build for iOS.

LibGDX is also capable of using GWT to compile and run in a
web browser. I have not tested GGVm with this and I have no
personal interest in distributing for web. But if anybody
expresses interest maybe I'll try to get that up and running
eventually.

# Setting up local.properties

The local.properties file is used to configure the build for all
platforms. local.properties.example has been provided for you to
give examples of what the various variables need to be set to.
The GGVm snapshot contains a rom and assets for Dushlan, an open
source NES game, to demo GGVm. Without a game and associated
java classes, GGVm will fail to build.

# GGVm's Build System

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

# Naming and Case Sensitivity

Note that GGVm's build system is case sensitive. Your assets
directory must be all lowercase. But all other instances of your
game name must exactly match what is in local.properties. In
other words, if your game is Dushlan, then each game specific
class that Dushlan needs MUST have the exact string Dushlan in
the name, or your build will fail. Please keep this in mind
through the rest of this document.

# The Assets Directory

The assets directory is where you place subdirectories for each
game you want to wrap up with ggvm, each which contain the game
rom, game-specific Java classes, and icons for display on the PC
executable, Android launcher and window. Depending on which
features you are using for your game, some of these directories
are optional. A description follows. Note the dushlan directory
is used as an example.

dushlan/dushlan.nes:
Not optional. This is the game's rom. The name can be anything,
see [An Example GameModule](#an-example-gamemodule).

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
inner class right inside your game's module. See
[Instructions for Creating an NmiSafeFunctor](#instructions-for-creating-an-nmisafefunctor).

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

# Building a Game for PC

To run the currently configured game (see [Setting up local.properties](#setting-up-localproperties)) on PC, type:

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

# Building a Game for Android

To run the currently configured game (see [Setting up local.properties](#setting-up-localproperties)) on Android, type:

```
./gradlew android:run
```

To build the currently configured game as an APK for Android,
type:

```
./gradlew android:assembleDebug
```

To build a release version type:

```
./gradlew android:assembleRelease
```

Note: How to sign your release APK in preparation for publishing
on the Google Play Store is outside the scope of this document.

# Building a Game for iOS

NOTE: .ogg sound files do not play back on iOS. It is recommended to use
.mp3 for all platforms if you plan to deploy on iOS. NOTE: The included
Dushlan assets are all .ogg files and will not run on iOS. I will update
this in the future.

To build and run your game for iOS, you will have to have a Mac, and an
Apple Developer account (99$). You will need to have a bundle identifier
set up in your Apple Developer account, and a provisioning profile. Then,
you need to modify your robovm.properties file, which is located in
ios/robovm.properties, so that app.id matches your bundle identifier.
In theory that should be all you need. To run your game in an iOS
simulator, you can type:

```
./gradlew ios:launchIPadSimulator
```

or

```
./gradlew ios:launchiPhoneSimulator
```

To run your game on your own iOS device, (note: The device's UUID must be
registered with your Apple Developer account or you will not be able to run
anything on it)

```
./gradlew ios:launchIOSDevice
```

To build an IPA file,

```
./gradlew ios:createIPA
```

Actually deploying an IPA file on the App Store is outside the scope of this
document.

# Instructions for Creating a Custom GameModule

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
interpret the iNES file for you. It is recommended to obfuscate the
iNES header in your ROM and configure from this method
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
ignored and can be zeroed out or filled with garbage in your ROM.

# An Example GameModule

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
import com.gradualgames.manager.render.RenderManager;
import com.gradualgames.manager.rastereffect.RasterEffectManager;
import com.gradualgames.manager.soundtrack.SoundtrackManager;
import com.gradualgames.manager.soundtrack.GGVmSoundtrackManager;
import com.gradualgames.manager.soundtrack.SongInfo;
import com.gradualgames.manager.render.HorizontalMirroringRenderManager;
import com.gradualgames.manager.render.VerticalMirroringRenderManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 1/7/2017.
 */
public class MyGameGameModule implements GameModule {
    @Override
    public String provideTitle() {
        return "My Game";
    }

    @Override
    public String provideFileName() {
        return "mygame/mygame.nes";
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
        //Note this must be changed to HorizontalMirroringRenderManager if your game uses
        //horizontal mirroring.
        return new VerticalMirroringRenderManager(ggvm, rasterEffectManager, true);
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
        List<SongInfo> songList = new ArrayList<SongInfo>();
        //Note this is sample data. Modify this list with your own paths.
        songList.add(new SongInfo("game/music/song_intro.mp3", "game/music/song.mp3", true));

        //Note this is sample data. Modify this list with your own paths.
        List<String> sfxList = new ArrayList<String>();
        sfxList.add("game/sfx/sfx00.mp3");

        //This is the recommended SoundtrackManager class to use. You may also
        //write your own. See the [Audio Playback Registers](#audio-playback-registers) section for how to
        //use this manager to play back the songs and sfx in the two lists above.
        return new GGVmSoundtrackManager("game", ggvm, songList, sfxList);
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
# Instructions for Creating an NmiSafeFunctor

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

# Virtual Registers Reference

GGVm provides a few additional registers on the cpu bus to enable
additional functionality. These are necessary since GGVm is not a
full NES emulator by design.

### Sprite 0 Hit Status Bar Register

Address: $5500

Usage: Write a nonzero value to turn on the sprite 0 hit status bar.
Write a value of 0 to turn off the sprite 0 hit status bar.

Behavior: The nametable above sprite 0's Y coordinate will be rendered
at scroll position 0, 0, from the first nametable. Below sprite 0's Y
coordinate, the scroll will be rendered at the current scroll position
in ppu register $2005. This feature has not yet been tested with a
status bar and vertical scrolling. Also, the split must be on an 8 pixel
grid boundary.

NOTE: To use this feature, it is highly recommended to remove actual
usage of Sprite 0 Hit from your game's ROM prior to using it with
ggvm. The Sprite 0 Hit bit in GGVm's PPU never activates; expect your
game to freeze execution if it is using Sprite 0 Hit normally.

NOTE: To enable this feature, you must pass "true" for the statusBarEnabled
parameter to the constructor of VerticalMirroringRenderManager or
HorizontalMirroringRenderManager. See the [An Example GameModule](#an-example-gamemodule).

### Audio Playback Registers

These registers will be installed on the cpuBus if you provide a
GGVmSoundtrackManager from your GameModule. GGVmSoundtrackManager must
be initialized with a list of SongInfo objects describing intro and
looping portions of songs, and a list of strings describing the paths
of sound effects. Here's an example of how to use this manager.

```
    @Override
    public SoundtrackManager provideSoundtrackManager(GGVm ggvm) {
        List<SongInfo> songList = new ArrayList<SongInfo>();
        //Note this is sample data. Modify this list with your own paths.
        songList.add(new SongInfo("game/music/song_intro.mp3", "game/music/song.mp3", true));

        //Note this is sample data. Modify this list with your own paths.
        List<String> sfxList = new ArrayList<String>();
        sfxList.add("game/sfx/sfx00.mp3");

        //This is the recommended SoundtrackManager class to use. You may also
        //write your own. See the [Audio Playback Registers](#audio-playback-registers) section for how to
        //use this manager to play back the songs and sfx in the two lists above.
        return new GGVmSoundtrackManager("game", ggvm, songList, sfxList);
    }
```

As a result of using this manager, new registers will be available
on the cpu bus for your game ROM to utilize. Here they are:

$5600 - Write a single byte to play a song from the song list.

$5601 - Write a single byte to play a sfx from the sfx list.

$5602 - Write a byte of any value to pause current music.

$5603 - Write a byte of any value to resume current music.

$5604 - Write a byte of any value to stop all music.

# Instructions for Creating a Custom SoundtrackManager

Note that creating your own custom SoundtrackManager is no longer
recommended. The easiest way to use GGVm's audio capabilities is
to use the special virtual audio registers documented above, via
GGVmSoundtrackManager.

A gamemodule must at least provide an anonymous inner class
with a no-op SoundtrackManager extension, as shown in the
GameModule section. However, if you want music and sound to play back
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
switch case which first inspects the address to see which routine
was called. Next, the soundtrack manager inspects the ram,
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
it is completed. Then the looping portion of the song,
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

A downside is the degree of control you have over audio playback.
GGVm supports most typical scenarios in use for NES homebrew
games, but a highly advanced sound system may be difficult or
impossible to fully replicate.

# Instructions for Creating a Custom RasterEffectManager

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

For split screen effects, GGVm provides some special registers for
performing split screens. Natively implemented scanline effects
are not supported. See [Virtual Registers Reference](#virtual-registers-reference).
These effects will likely be expanded in ongoing development of GGVm.

# Gotchas

If any of the following happen when you try your ROM in GGVm, here
is what you should do.

- The game totally freezes.
    - Are you using Sprite 0 Hit? GGVm does not natively support this
bit. It does however support Sprite 0 Hit based status bars via a
special register. See [Virtual Registers Reference](#virtual-registers-reference).

- Inexplicable odd behavior, glitches, etc.
    - There are a couple of things that can cause this. The most likely
thing is that your game was tuned to the precise timing of an NES,
and nmi is stepping on something and causing chaos. It is recommended
to make your game as robust as possible, but if you would prefer to
leave your NES rom unmodified, see [Instructions for Creating an NmiSafeFunctor](#instructions-for-creating-an-nmisafefunctor),
as it can work around many issues like this.
    - Another possibility is that your game is attempting to detect NTSC,
Pal or Dendy. GGVm will cause incorrect detection in this type of code. You
will need to modify your ROM to force the tv type to NTSC, since GGVM operates
at 60 fps. Depending on the game, tvSystem may be used to look up different
speed values etc.

- A small gap between intro and looping portions of a song, or a gap while looping.
    - There are several things you can try to improve this situation. OGG typically works
    well on most systems (unsupported on iOS, however). You need to make sure there are no
    silent samples at the beginning or end of your looping portion and no silent samples at
    the end of your intro portion. Audacity works well for this type of audio editing.
    - If gaps still occur, this could be due to differences in audio hardware and software
    on different PCs. The best workaround for this is to create a version of your song which
    contains the intro and several iterations of the looping portion, followed by just the
    looping portion. If you make this long enough with your game's style of gameplay in mind,
    the player may never notice the gap occuring unless they are really paying attention.

- Something else?
    - Please log an issue here: https://github.com/gradualgames/ggvm/issues. When providing
information about your issue, please include log.txt. This file will be saved in the working
directory where your game was running. During development typically this will be in android/assets
(this is shared between android and desktop projects).
