/*-
 * #%L
 * FXFileChooser
 * %%
 * Copyright (C) 2017 - 2022 Oliver Loeffler, Raumzeitfalle.net
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.raumzeitfalle.fx.filechooser;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

final class FindFilesTask extends Task<Integer> {

    private final ObservableList<IndexedPath> pathsToUpdate;

    private final Path directory;

    private final DoubleProperty duration;

    public FindFilesTask(Path searchFolder, ObservableList<IndexedPath> listOfPaths) {
        this.pathsToUpdate = Objects.requireNonNull(listOfPaths, "listOfPaths must not be null");
        this.directory = searchFolder;
        this.duration = new SimpleDoubleProperty(0d);
    }

    /**
     * Even in case the directory to be processed is empty or does not exist, the
     * consumer collection is always cleared as first step.
     * 
     * @return number of files found and processed
     */
    @Override
    protected Integer call() throws Exception {
        Invoke.andWait(pathsToUpdate::clear);
        long start = System.currentTimeMillis();
        if (null == directory) {
            return 0;
        }

        File[] files = directory.toAbsolutePath().toFile().listFiles();
        if (null == files) {
            return 0;
        }

        if (files.length == 0)
            return 0;

        updateProgress(0, files.length);
        int progressIntervall = getProgressInterval(files.length);
        RefreshBuffer buffer = RefreshBuffer.get(this, files.length, pathsToUpdate);
        for (int f = 0; f < files.length; f++) {
            if (isCancelled()) {
                updateProgress(f, files.length);
                duration.set((System.currentTimeMillis() - start) / 1E3);
                buffer.flush();
                break;
            }
            if (f % progressIntervall == 0) {
                updateProgress(f + 1, files.length);
            }
            if (files[f].isFile()) {
                buffer.update(files[f].toPath());
            }
        }
        buffer.flush();
        updateProgress(files.length, files.length);
        duration.set((System.currentTimeMillis() - start) / 1E3);
        return files.length;
    }

    @Override
    protected void running() {
        super.running();
        Logger.getLogger(FindFilesTask.class.getName()).log(Level.INFO, "in {0}",
                directory.normalize().toAbsolutePath());
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        Logger.getLogger(FindFilesTask.class.getName()).log(Level.INFO,
                "with {0} files out of {1} entries after {2} sec",
                new Object[] {pathsToUpdate.size(), getValue(), duration.get()});
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        Logger.getLogger(FindFilesTask.class.getName()).log(Level.INFO, "with {0} files after {1} seconds!",
                new Object[] {pathsToUpdate.size(), duration.get()});
    }

    @Override
    protected void failed() {
        super.failed();
        String message = String.format("after indexing %s files with an error.", pathsToUpdate.size());
        Logger.getLogger(FindFilesTask.class.getName()).log(Level.WARNING, message, getException());
    }

    protected int getProgressInterval(int length) {
        int divider = 1;
        if (length >= 200) {
            divider = length / 200;
        }
        return divider;
    }
}
