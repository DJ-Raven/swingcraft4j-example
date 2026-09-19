package com.swingcraft4j;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.FontUtils;
import com.swingcraft4j.treehoveraction.HoverAction;
import com.swingcraft4j.treehoveraction.HoverActionTree;
import com.swingcraft4j.treehoveraction.event.TreeDropListener;
import com.swingcraft4j.treehoveraction.model.Item;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Demo entry point: wires up a {@link HoverActionTree} with sample data.
 */
public class Main extends JFrame {

    public Main() {
        super("Tree Hover Action");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        init();
    }

    private void init() {
        JPanel panel = new JPanel(new MigLayout("fill", "[fill]", "[fill]"));

        JSplitPane splitPane = new JSplitPane();

        HoverActionTree treeHover = new HoverActionTree() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        treeHover.setRootVisible(false);

        JScrollPane scrollPane = new JScrollPane(treeHover);

        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(new JLabel("Empty", SwingConstants.CENTER));

        splitPane.setDividerLocation(270);

        // style
        treeHover.putClientProperty(FlatClientProperties.STYLE_CLASS, "menu");
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "" +
                "background:$Tree.background;" +
                "border:5,5,5,5,$Component.borderColor,1,10;");
        splitPane.putClientProperty(FlatClientProperties.STYLE, "style:plain;");

        // init action
        HoverAction hoverAction = new HoverAction(new HoverAction.Item[]{
                new HoverAction.Item(new FlatSVGIcon("com/swingcraft4j/treehoveraction/icons/plus.svg", 0.8f), "Add"),
                new HoverAction.Item(new FlatSVGIcon("com/swingcraft4j/treehoveraction/icons/edit.svg", 0.8f), "Edit"),
                new HoverAction.Item(new FlatSVGIcon("com/swingcraft4j/treehoveraction/icons/more.svg", 0.8f), "More action")
        });
        hoverAction.setActionVisible((actionIndex, leaf, row) -> {
            if (leaf && actionIndex == 0) {
                // invisible add action if node current leaf
                return false;
            }
            return true;
        });
        treeHover.setHoverAction(hoverAction);

        // event
        treeHover.addHoverActionListener((evt, row, index) -> {
            System.out.println("action changed: " + row + " " + index);
            if (index == 0) {
                TreePath path = treeHover.getPathForRow(row);
                if (path != null) {
                    MutableTreeNode parent = (MutableTreeNode) path.getLastPathComponent();
                    DefaultTreeModel model = (DefaultTreeModel) treeHover.getModel();
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("New folder");
                    model.insertNodeInto(newNode, parent, 0);
                    treeHover.expandPath(path);
                    treeHover.startEditingAtPath(new TreePath(newNode.getPath()));
                }
            } else if (index == 1) {
                treeHover.startEditingAtPath(treeHover.getSelectionPath());
            } else if (index == 2) {
                showMoreMenu(treeHover, evt, row);
            }
        });

        // drag & drop
        treeHover.addTreeDropListener(new TreeDropListener() {
            @Override
            public boolean canDrop(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode newParent, int index) {
                if (newParent == treeHover.getModel().getRoot()) {
                    // root is hidden - don't let items land at the top level, outside any category
                    return false;
                }
                // only folder-like nodes (plain String user objects) accept children; leaf
                // Item nodes can't - this is just one example of an app-specific placement rule
                return !(newParent.getUserObject() instanceof Item);
            }

            @Override
            public boolean nodesDropped(List<DefaultMutableTreeNode> nodes, DefaultMutableTreeNode newParent, int dropIndex) {
                if (!canDrop(nodes, newParent, dropIndex)) {
                    return false;
                }
                StringBuilder sb = new StringBuilder("dropped [");
                for (int i = 0; i < nodes.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(nodes.get(i).getUserObject());
                }
                sb.append("] into \"").append(newParent.getUserObject()).append("\" at index ").append(dropIndex);
                System.out.println(sb);
                return true;
            }
        });

        sampleData(treeHover);
        panel.add(splitPane);
        add(panel);
    }

    /**
     * Popup shown from the "more" hover action: view the node in the console, or delete it.
     */
    private void showMoreMenu(HoverActionTree treeHover, MouseEvent evt, int row) {
        TreePath path = treeHover.getPathForRow(row);
        if (path == null) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        JPopupMenu menu = new JPopupMenu();

        JMenuItem viewDetails = new JMenuItem("View details");
        viewDetails.addActionListener(e -> System.out.println("Details: " + node.getUserObject()));
        menu.add(viewDetails);

        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(e -> {
            DefaultTreeModel model = (DefaultTreeModel) treeHover.getModel();
            model.removeNodeFromParent(node);
        });
        menu.add(delete);

        menu.show(treeHover, evt.getX(), evt.getY());
    }

    /**
     * Placeholder tree content for the demo window - arbitrary categories/items, not meaningful.
     */
    private void sampleData(HoverActionTree treeHover) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        DefaultMutableTreeNode backend = new DefaultMutableTreeNode("Backend");
        backend.add(new DefaultMutableTreeNode(new Item(Item.Type.JAVA, "Java")));
        backend.add(new DefaultMutableTreeNode(new Item(Item.Type.C_SHARP, "C#")));
        backend.add(new DefaultMutableTreeNode(new Item(Item.Type.NODEJS, "Node.js")));

        DefaultMutableTreeNode scripting = new DefaultMutableTreeNode("Scripting");
        scripting.add(new DefaultMutableTreeNode(new Item(Item.Type.PYTHON, "Python")));
        backend.add(scripting);

        DefaultMutableTreeNode systems = new DefaultMutableTreeNode("Systems");
        systems.add(new DefaultMutableTreeNode(new Item(Item.Type.C, "C")));
        systems.add(new DefaultMutableTreeNode(new Item(Item.Type.CPP, "C++")));

        DefaultMutableTreeNode frontend = new DefaultMutableTreeNode("Frontend");
        frontend.add(new DefaultMutableTreeNode(new Item(Item.Type.HTML_5, "HTML5")));
        frontend.add(new DefaultMutableTreeNode(new Item(Item.Type.FLUTTER, "Flutter")));

        DefaultMutableTreeNode database = new DefaultMutableTreeNode("Database");
        DefaultMutableTreeNode sql = new DefaultMutableTreeNode("SQL");
        sql.add(new DefaultMutableTreeNode(new Item(Item.Type.MYSQL, "MySQL")));
        sql.add(new DefaultMutableTreeNode(new Item(Item.Type.MARIADB, "MariaDB")));
        database.add(sql);

        DefaultMutableTreeNode devOps = new DefaultMutableTreeNode("DevOps");
        devOps.add(new DefaultMutableTreeNode(new Item(Item.Type.DOCKER, "Docker")));

        root.add(backend);
        root.add(systems);
        root.add(frontend);
        root.add(database);
        root.add(devOps);

        treeHover.setModel(new DefaultTreeModel(root));
    }

    /**
     * Installs the demo's font/theme, then shows the window on the Swing event thread.
     */
    public static void main(String[] args) {
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("com.swingcraft4j.treehoveraction.themes");
        UIManager.put("defaultFont", FontUtils.getCompositeFont(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatMacDarkLaf.setup();

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
