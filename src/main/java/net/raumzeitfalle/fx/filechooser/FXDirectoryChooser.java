/*-
 * #%L
 * FXFileChooser
 * %%
 * Copyright (C) 2017 - 2019 Oliver Loeffler, Raumzeitfalle.net
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
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/*
 * TODO: Functionality will become internal-API.
 * TODO: Remove the old style FX Directory Chooser.
 */
public class FXDirectoryChooser implements PathSupplier {

    public static FXDirectoryChooser createIn(ObjectProperty<Path> startLocation, Supplier<Window> ownerProvider) {
        Objects.requireNonNull(startLocation, "startLocation must not be null");
        Path location = startLocation.get();
        if (null == location) {
            location = Paths.get("./");
        }
        return new FXDirectoryChooser(location, ownerProvider);
    }

    public static FXDirectoryChooser createIn(Path startLocation, Supplier<Window> ownerProvider) {
        Objects.requireNonNull(ownerProvider, "ownerProvider must not be null");
        Objects.requireNonNull(startLocation, "startLocation for file search must not be null.");
        Path location = startLocation;
        if (String.valueOf(startLocation).equals("")) {
            location = Paths.get("./");
        }
        return new FXDirectoryChooser(location, ownerProvider);
    }

    private final DirectoryChooser dc;

    private final Supplier<Window> ownerProvider;

    private FXDirectoryChooser(Path startLocation, Supplier<Window> ownerProvider) {
        this.dc = new DirectoryChooser();
        this.dc.setInitialDirectory(startLocation.toFile());
        this.ownerProvider = Objects.requireNonNull(ownerProvider, "ownerProvider must not be null");
    }

    public void getUpdate(Consumer<Path> update) {
        Platform.runLater(() -> Optional.ofNullable(dc.showDialog(ownerProvider.get()))
                                        .map(File::toPath)
                                        .ifPresent(update.andThen(this::changeDir)));
    }

    private void changeDir(Path newLocation) {
        this.dc.setInitialDirectory(newLocation.toFile());
    }
}
