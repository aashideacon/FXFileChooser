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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.FilteredList;
import net.raumzeitfalle.fx.filechooser.locations.Location;

final class FileChooserModel {
    
    private final ObservableList<IndexedPath> allPaths;
    
    private final FilteredList<IndexedPath> filteredPaths;

    private UpdateService fileUpdateService;
    
    private final ListProperty<IndexedPath> allPathsProperty;
        
    private final ListProperty<IndexedPath> filteredPathsProperty;
    
    private final ObjectProperty<Path> fileSelection = new SimpleObjectProperty<>();

    private final StringProperty selectedFileName = new SimpleStringProperty("");

    private final BooleanProperty invalidSelection = new SimpleBooleanProperty(true);
    		
    private final ObservableList<PathFilter> observablePathFilter = FXCollections.observableArrayList(new ArrayList<>(30));

    private final ObservableSet<Location> locations = FXCollections.observableSet(new LinkedHashSet<>());

    private PathFilter effectiveFilter = PathFilter.acceptAllFiles("all files");    

    public static FileChooserModel startingInUsersHome(PathFilter ...filter) {
        return startingIn(getUsersHome(), filter);
    }
    
    public static FileChooserModel startingIn(Path startFolder, PathFilter ...filter) {
    		ObservableList<IndexedPath> paths = FXCollections.observableArrayList(new ArrayList<>(300_000));
    		Supplier<UpdateService> serviceProvider = ()->new FileUpdateService(startFolder, paths);
    		FileChooserModel model = new FileChooserModel(paths, serviceProvider);
    		model.observablePathFilter.addAll(filter);
    		return model;
    }
    
    public FileChooserModel(ObservableList<IndexedPath> paths, Supplier<UpdateService> serviceProvider) {
        this.allPaths = paths;
        this.filteredPaths = new FilteredList<>(allPaths);
        this.allPathsProperty = new SimpleListProperty<>(this.allPaths);
        this.filteredPathsProperty = new SimpleListProperty<>(this.filteredPaths);
        this.fileUpdateService = serviceProvider.get();
        this.fileUpdateService.startUpdate();
        this.selectedFileName.bind(createStringBindingTo(fileSelection));
        this.initializeFilter("");

    }

    private StringBinding createStringBindingTo(ObservableValue<?> observable) {
        Callable<String> callable =
                ()->(null != observable.getValue()) ? String.valueOf(observable.getValue()) : "";

        return Bindings.createStringBinding(callable, observable);
    }

    private static Path getUsersHome() {
        return Paths.get(System.getProperty("user.home"));
    }
    
    public UpdateService getUpdateService() {
        return this.fileUpdateService;
    }
    
    public ObjectProperty<Path> currentSearchPath() {
        return this.fileUpdateService.searchPathProperty();
    }

    public ObservableList<IndexedPath> getFilteredPaths() {
        return filteredPaths;
    }
    
    ReadOnlyIntegerProperty filteredPathsSizeProperty() {
        return this.filteredPathsProperty.sizeProperty();
    }
    
    ReadOnlyIntegerProperty allPathsSizeProperty() {
        return this.allPathsProperty.sizeProperty();
    }
    
    ReadOnlyBooleanProperty invalidSelectionProperty() {
        return this.invalidSelection;
    }

    public void setSelectedFile(IndexedPath file) {
        if (null == file) {
            this.fileSelection.setValue(null);
        } else {
            this.fileSelection.setValue(file.asPath().toAbsolutePath().normalize());
        }       
        this.invalidSelection.setValue(null == file);
    }
    
    public Path getSelectedFile() {
        return this.fileSelection.getValue();
    }
    
    public ReadOnlyObjectProperty<Path> selectedFileProperty() {
        return this.fileSelection;
    }

    public ReadOnlyStringProperty selectedFileNameProperty() {
        return this.selectedFileName;
    }
    
    /**
     * Updates only the live filter criterion, not the effective path filter.
     * @param criterion {@link String} A search text such as &quot;index&quot; for &quot;index.html&quot; or &quot;index.txt&quot;.
     */
    public void updateFilterCriterion(String criterion) {
        Predicate<IndexedPath> customFilter = createManualListFilter(criterion);                           
        this.filteredPaths.setPredicate(combineFilterPredicates(customFilter));
    }

    /**
     * Updates the effective {@link PathFilter} and the the live filter criterion.
     * @param pathFilter {@link PathFilter} Usually a specific file filter such as *.txt or *.html.
     * @param criterion {@link String} A search text such as &quot;index&quot; for &quot;index.html&quot; or &quot;index.txt&quot;.
     */
    public void updateFilterCriterion(PathFilter pathFilter, String criterion) {
    		this.effectiveFilter = pathFilter;
    		updateFilterCriterion(criterion);
    }
    
    private Predicate<IndexedPath> createManualListFilter(String criterion) {
        String corrected = removeInvalidChars(criterion);
        return p -> null == corrected 
        				|| corrected.isEmpty() 
        				|| p.toString().toLowerCase().contains(corrected.toLowerCase());
    }

    private Predicate<IndexedPath> combineFilterPredicates(Predicate<IndexedPath> customFilter) {
    	
    		Predicate<Path> effective = this.effectiveFilter.getPredicate();
    	
    		List<Predicate<IndexedPath>> predicates = new ArrayList<>();
    		predicates.add(indexedPath -> effective.test(indexedPath.asPath()));
    		predicates.add(customFilter);

        return predicates
        				.stream()
        				.reduce(x -> true, Predicate::and);
        
    }
    
    public void initializeFilter(String text) {
    		if (!this.observablePathFilter.isEmpty()) {
    			PathFilter combined = this.observablePathFilter.get(0);
    			for (PathFilter filter : this.observablePathFilter) {
    				combined = combined.combine(filter);
    			}
    			 this.effectiveFilter = combined;
    		}
    		updateFilterCriterion(text);
    }

    private String removeInvalidChars(String criterion) {
        char[] invalidChars = new char[] {'"','?','<','>','|',':','*'};   
        String corrected = criterion;
        for (char invalid : invalidChars) {
            corrected = corrected.replace(String.valueOf(invalid), "");
        }
        return corrected;
    }
    
    public void refreshFiles() {
        this.fileUpdateService.refresh();
    }
    
    public void updateFilesIn(File directory) {
        if (null != directory) {
            updateFilesIn(directory.toPath());    
        }
    }

    public void updateFilesIn(Location location) {
        if (null != location) {
            updateFilesIn(location.getPath());
        }
    }

	public void updateFilesIn(Path directory) {
		if (directory.toFile().isDirectory()) {
			this.fileUpdateService.restartIn(directory);

		} else if (directory.toFile().isFile()) {
			Path parent = directory.getParent();
			if (parent != null) {
				this.fileUpdateService.restartIn(parent);
			}
		}
	}
    
    public void changeToUsersHome() {
        updateFilesIn(getUsersHome());
    }

    public ObservableList<PathFilter> getPathFilter() {
    	return this.observablePathFilter;
    }

    public void addLocation(Location location) {
        this.locations.add(location);
    }

    public ObservableSet<Location> getLocations() {
        return this.locations;
    }

	public void sort(Comparator<IndexedPath> comparator) {
	    this.allPaths.sort(comparator);
	}

	public void addOrRemoveFilter(PathFilter newFilter) {
		boolean wasRemoved = this.observablePathFilter.removeIf(pf->pf.getName().equalsIgnoreCase(newFilter.getName()));
		if (!wasRemoved) {
			this.observablePathFilter.add(newFilter);
		}
	}
}
