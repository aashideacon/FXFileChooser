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
package net.raumzeitfalle.fx.demos;

import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.raumzeitfalle.fx.filechooser.PathFilter;
import net.raumzeitfalle.fx.filechooser.Skin;
import net.raumzeitfalle.fx.filechooser.SwingFileChooser;
import net.raumzeitfalle.fx.filechooser.locations.Location;
import net.raumzeitfalle.fx.filechooser.locations.Locations;

public class DemoJavaSwingIntegration {

    public static void main(String[] args) {
        DemoJavaSwingIntegration app = new DemoJavaSwingIntegration();
        SwingUtilities.invokeLater(app::initAndShowGui);
    }

    private final JPanel buttonHolder;

    private DemoJavaSwingIntegration() {
        this.buttonHolder = createButtonHolder();
    }

    private List<PathFilter> collectPathFilter() {
        PathFilter all = PathFilter.acceptAllFiles("all files");

        PathFilter xml = PathFilter.forFileExtension("eXtensible Markup Language (xml)", "xml");
        PathFilter txt = PathFilter.forFileExtension("TXT", "txt");

        PathFilter pdf = PathFilter.forFileExtension("PDF: Portable Document Format", "pdf");
        PathFilter png = PathFilter.forFileExtension("*.png", "png");

        PathFilter svg = PathFilter.forFileExtension("Scalable Vector Graphics (*.svg)", "svg");

        PathFilter html = PathFilter.forFileExtension("*.html", "html")
                .combine(PathFilter.forFileExtension("*.htm", "html"));

        PathFilter xls = PathFilter.forFileExtension("*.xls", "xls")
                .combine(PathFilter.forFileExtension("*.xlsx", "xlsx"));

        PathFilter htmlAndExcel = html.combine(xls).combine(png);

        return Arrays.asList(all, xml, txt, pdf, png, svg, html, xls, htmlAndExcel);
    }

    private void initAndShowGui() {
        JFrame frame = new JFrame("JavaFX in Swing");

        // JavaFX stage placed inside a JDialog - default skin

        PathFilter[] filter = collectPathFilter().toArray(new PathFilter[0]);
        SwingFileChooser fc = SwingFileChooser.create(Skin.MODENA, "Choose any file:", "TestData/SomeFiles", filter);

        String title = "<html><center><h3>JavaFX Stage inside Swing JDialog (with Locations)</h3>"
                + SwingFileChooser.class.getName() + "<br>" + "<font color=#0000FF>(Default Skin)</font>"
                + "</center></html>";

        List<Location> locations = new ArrayList<>();
        locations.add(Locations.withName("Configs: /etc", Paths.get("/etc")));
        locations.add(Locations.withName("User Homes: /Users", Paths.get("/Users")));
        locations.add(Locations.withName("C-Drive: C:\\", Paths.get("C:/")));

        fc.addLocations(locations);

        Example stageInsideSwingDialog = new Example(title, () -> fc.showOpenDialog(frame), () -> fc.getSelectedFile());

        addExample(stageInsideSwingDialog);

        // JavaFX stage placed inside a JDialog - dark skin
        title = "<html><center><h3>JavaFX Stage inside Swing JDialog</h3>" + SwingFileChooser.class.getName() + "<br>"
                + "<font color=#0000FF>(Dark Skin)</font>" + "</center></html>";
        SwingFileChooser darkFc = SwingFileChooser.create(Skin.DARK, "Choose any file:", "TestData/SomeFiles", filter);

        Example stageInsideSwingDialogDark = new Example(title, () -> darkFc.showOpenDialog(frame),
                () -> darkFc.getSelectedFile());

        addExample(stageInsideSwingDialogDark);

        JFileChooser swingFc = new JFileChooser();
        title = "<html><center><h3>Java Swing FileChooser</h3>" + JFileChooser.class.getName() + "<br>"
                + "<font color=#0000FF></font>" + "</center></html>";
        Example swingWay = new Example(title, () -> swingFc.showOpenDialog(frame), () -> swingFc.getSelectedFile());
        addExample(swingWay);

        frame.getContentPane().add(buttonHolder);
        frame.pack();
        frame.setSize(900, 700);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private JPanel createButtonHolder() {
        GridLayout gridLayout = new GridLayout(6, 3);
        gridLayout.setHgap(20);
        gridLayout.setVgap(10);

        return new JPanel(gridLayout);
    }

    private void addExample(Example example) {
        this.buttonHolder.add(example.getButton());
        this.buttonHolder.add(example.getLabel());
    }

    private static class Example {

        private final JLabel label;
        private final JButton button;

        public Example(String title, IntSupplier interaction, Supplier<File> fileSource) {

            this.label = new JLabel();
            this.button = new JButton(title);

            this.button.addActionListener(l -> {
                int option = interaction.getAsInt();
                if (option == JFileChooser.APPROVE_OPTION) {
                    SwingUtilities.invokeLater(() -> {
                        File file = fileSource.get();
                        String text = (null == file) ? "" : String.valueOf(file);
                        label.setText(text);
                    });
                }
            });
        }

        JButton getButton() {
            return button;
        }

        JLabel getLabel() {
            return label;
        }
    }
}
