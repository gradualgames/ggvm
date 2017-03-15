package com.gradualgames.application;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.OnGenerateGraphicsListener;
import com.gradualgames.input.InputProcessorBase;
import com.gradualgames.manager.rastereffect.RasterEffectManager;
import com.gradualgames.manager.render.RenderManager;
import com.gradualgames.manager.soundtrack.SoundtrackManager;
import com.gradualgames.menu.Menu;
import com.gradualgames.module.GameModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is the GGVmApplication object, specific to integration with LibGDX as the game
 * framework. It takes in a game module, input processor type and menu type from the
 * platform specific launcher. It initializes the GGVm virtual machine, input processor and
 * menu. On every frame, it forwards the render call to the render manager, input processor
 * (in case of overlay in mobile) and menu. On every frame, it calls nmi and advances ggvm by
 * an arbitrary number of instructions that approximates how many instructions the NES would
 * execute, per frame. It also kicks off saving and loading a savestate of the current game
 * depending on the application lifecycle. Finally, it manages logging some heap information
 * and printing uncaught exceptions, cpu status and current bank (for supported mappers) to
 * the log file in case of a catastrophic crash.
 */
public class GGVmApplication extends ApplicationAdapter implements OnGenerateGraphicsListener, Thread.UncaughtExceptionHandler {
    //The GGVm virtual machine object, this is the integration point to our NES games.
    private GGVm ggvm;

    //LibGDX objects
    private SpriteBatch spriteBatch;

    //The menu manager
    private Class<? extends Menu> menuClass;
    private Menu menu;

    //Game specific module and managers
    private GameModule gameModule;
    private RenderManager renderManager;
    private RasterEffectManager rasterEffectManager;
    private SoundtrackManager soundtrackManager;

    //Input processor objects
    private Class<? extends InputProcessorBase> inputProcessorClass;
    private InputProcessorBase inputProcessor;

    //Heap info logging
    private static final int HEAP_INFO_COUNTER_RESET = 500;
    private int heapInfoCounter = HEAP_INFO_COUNTER_RESET;

    public GGVmApplication(GameModule gameModule, Class<? extends Menu> menuClass, Class<? extends InputProcessorBase> inputProcessorClass) {
        this.gameModule = gameModule;
        this.menuClass = menuClass;
        this.inputProcessorClass = inputProcessorClass;
    }

    @Override
    public void create() {
        Thread.currentThread().setUncaughtExceptionHandler(this);

        //Set up logging, window title and turn on vsync
        Gdx.app.setLogLevel(Gdx.app.LOG_INFO);
        Gdx.app.log(getClass().getSimpleName(), "create()");
        Gdx.graphics.setTitle(gameModule.provideTitle());
        Gdx.graphics.setVSync(true);

        //Initialize ggvm
        FileHandle fileHandle = Gdx.files.internal(gameModule.provideFileName());
        ggvm = new GGVm(gameModule.provideCartridge(fileHandle.readBytes()), gameModule.provideNmiSafeFunctor(), this);

        //Initialize game-specific classes that depend on ggvm
        rasterEffectManager = gameModule.provideRasterEffectManager(ggvm);
        soundtrackManager = gameModule.provideSoundtrackManager(ggvm);

        //Initialize input processor
        inputProcessor = InputProcessorBase.newInstance(inputProcessorClass, ggvm);
        inputProcessor.loadConfiguration();
        Gdx.input.setInputProcessor(inputProcessor);
        Controllers.addListener(inputProcessor);

        //Initialize menu manager
        menu = Menu.newInstance(menuClass);
        menu.setDependencies(gameModule.provideFontFileName(), gameModule.provideFontBitmapFileName(), ggvm, inputProcessor, soundtrackManager);
        menu.create();
        inputProcessor.setMenu(menu);

        //Get render strategy and create all LibGDX objects
        renderManager = gameModule.provideRenderManager(ggvm, rasterEffectManager);

        //Setup application level LibGDX objects
        spriteBatch = new SpriteBatch();

        loadState();

        //Generate graphics at least once in case we have a CHR-ROM mapper.
        renderManager.onGenerateGraphics();

        ggvm.start();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        Gdx.app.log(getClass().getSimpleName(), "resize(" + width + "," + height + ")");
        renderManager.resize(width, height);
        inputProcessor.resize(width, height);
        menu.resize(width, height);
    }

    @Override
    public void render() {
        renderManager.render(spriteBatch);
        inputProcessor.render(spriteBatch);
        menu.render(spriteBatch);
        ggvm.nmi();
        ggvm.advance(9000);
        ggvm.logInstructionsPerSecond();
        logHeapInformation();
    }

    @Override
    public void dispose() {
        Gdx.app.log(getClass().getSimpleName(), "dispose()");
        ggvm.stop();
        saveState();
    }

    /**
     * Logs heap information in megabytes each time heapInfoCounter reaches
     * zero from HEAP_INFO_COUNTER_RESET. Helps reveal the presence of
     * memory leaks during development.
     */
    private void logHeapInformation() {
        long heapMaxSize = Runtime.getRuntime().maxMemory() >> 20;
        long heapSize = Runtime.getRuntime().totalMemory() >> 20;
        long heapFreeSize = Runtime.getRuntime().freeMemory() >> 20;
        heapInfoCounter--;
        if (heapInfoCounter == 0) {
            heapInfoCounter = HEAP_INFO_COUNTER_RESET;
            Gdx.app.log(getClass().getSimpleName(), "********************************");
            Gdx.app.log(getClass().getSimpleName(), "Max heap size:  " + heapMaxSize + "MiB");
            Gdx.app.log(getClass().getSimpleName(), "Heap size:      " + heapSize + "MiB");
            Gdx.app.log(getClass().getSimpleName(), "Free heap size: " + heapFreeSize + "MiB");
            Gdx.app.log(getClass().getSimpleName(), "********************************");
        }
    }

    /**
     * Saves GGVm's current state to a save state file. This is used
     * when the user quits GGVm so they can resume their game where they
     * left off.
     * TODO: Implement for mobile devices.
     */
    private void saveState() {
        Gdx.app.log(getClass().getSimpleName(), "saveState()");
        try {
            FileHandle file = Gdx.files.local("state.sav");
            OutputStream outputStream = file.write(false);
            ggvm.saveState(outputStream);
            soundtrackManager.save(outputStream);
        } catch (IOException ex) {
            Gdx.app.error(getClass().getSimpleName(), "Error saving game state.", ex);
        }
    }

    /**
     * Loads last saved save state file.
     * TODO: Implement for mobile devices.
     */
    private void loadState() {
        Gdx.app.log(getClass().getSimpleName(), "loadState()");
        try {
            FileHandle file = Gdx.files.local("state.sav");
            if (file.exists()) {
                InputStream inputStream = file.read();
                ggvm.loadState(inputStream);
                soundtrackManager.load(inputStream);
            }
        } catch (IOException ex) {
            Gdx.app.error(getClass().getSimpleName(), "Error loading game state.", ex);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        ggvm.printRegisters();
        Gdx.app.log(getClass().getSimpleName(), "Bank: " + ggvm.getLowerPrgBank());
        Gdx.app.error(getClass().getSimpleName(), e.getMessage(), e);
        System.exit(-1);
    }

    @Override
    public void onGenerateGraphics() {
        renderManager.onGenerateGraphics();
    }
}
