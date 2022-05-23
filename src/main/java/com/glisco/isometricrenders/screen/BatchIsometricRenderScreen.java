package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.util.ImageExporter;
import com.glisco.isometricrenders.util.RuntimeConfig;
import com.glisco.isometricrenders.util.Translate;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import static com.glisco.isometricrenders.util.RuntimeConfig.exportResolution;
import static com.glisco.isometricrenders.util.RuntimeConfig.useExternalRenderer;

public abstract class BatchIsometricRenderScreen<T> extends IsometricRenderScreen {

    protected final Iterator<T> renderObjects;
    protected final int delay;
    protected int delayTicks;
    protected boolean invalid = false;
    protected String name;

    public BatchIsometricRenderScreen(Iterator<T> renderObjects, String name) {
        this.renderObjects = renderObjects;
        this.drawBackground = true;
        this.name = name;

        if (ImageExporter.Threaded.busy()) {
            Translate.chat("threaded_export_system_not_available");
            invalid = true;
        }

        ImageExporter.Threaded.init();

        if (useExternalRenderer && exportResolution > 2048 && !RuntimeConfig.allowInsaneResolutions) {
            Translate.chat("resolution_not_supported");
            invalid = true;
        }

        delay = (int) Math.pow(exportResolution / 1024f, 2);
    }

    @Override
    protected void init() {
        super.init();
        children().stream().filter(element -> element instanceof ClickableWidget).forEach(element -> ((ClickableWidget) element).active = false);
    }

    @Override
    public void close() {
        super.close();
        ImageExporter.Threaded.finish();
    }

    @Override
    protected CompletableFuture<File> addImageToExportQueue(NativeImage image) {
        return ImageExporter.Threaded.submit(image, currentFilename);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (invalid) {
            this.close();
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
        }
        if (ImageExporter.Threaded.acceptsNew() && delayTicks == 0) {
            setupRender();
            delayTicks = delay;
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    protected abstract void setupRender();

}