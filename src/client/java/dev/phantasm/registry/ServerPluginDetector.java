package dev.phantasm.registry;

/*Tracks which custom-content plugins are active on the current server*/
public final class ServerPluginDetector {

    private static final ServerPluginDetector INSTANCE = new ServerPluginDetector();

    private boolean modelEngine  = false;
    private boolean oraxen       = false;
    private boolean itemsAdder   = false;
    private boolean nexo         = false;

    private ServerPluginDetector() {}

    public static ServerPluginDetector get() { return INSTANCE; }


    public void detectModelEngine()  { if (!modelEngine)  { modelEngine  = true; log("ModelEngine"); } }
    public void detectOraxen()       { if (!oraxen)       { oraxen       = true; log("Oraxen"); } }
    public void detectItemsAdder()   { if (!itemsAdder)   { itemsAdder   = true; log("ItemsAdder"); } }
    public void detectNexo()         { if (!nexo)         { nexo         = true; log("Nexo"); } }


    public boolean isOraxenLatched()       { return oraxen; }
    public boolean isItemsAdderLatched()   { return itemsAdder; }
    public boolean isNexoLatched()         { return nexo; }
    public boolean isModelEngineLatched()  { return modelEngine; }

    //entity-based fallbacks

    public boolean hasModelEngine() {

        if (!modelEngine && ModelEngineRegistry.get().getBoneCount() > 0) {
            detectModelEngine();
        }
        return modelEngine;
    }

    public boolean hasOraxen() {
        if (!oraxen && FurnitureRegistry.get().hasOraxenFurniture()) {
            detectOraxen();
        }
        return oraxen;
    }

    public boolean hasItemsAdder() {
        if (!itemsAdder && FurnitureRegistry.get().hasItemsAdderFurniture()) {
            detectItemsAdder();
        }
        return itemsAdder;
    }

    public boolean hasNexo() {
        if (!nexo && FurnitureRegistry.get().hasNexoFurniture()) {
            detectNexo();
        }
        return nexo;
    }

    public boolean hasAnyPlugin() {
        return hasModelEngine() || hasOraxen() || hasItemsAdder() || hasNexo();
    }

    public void reset() {
        modelEngine = false;
        oraxen      = false;
        itemsAdder  = false;
        nexo        = false;
    }

    private void log(String plugin) {
        dev.phantasm.PhantasmClient.LOGGER.info("[Phantasm] Detected plugin: {}", plugin);
    }
}
