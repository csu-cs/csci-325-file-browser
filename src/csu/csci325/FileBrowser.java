package csu.csci325;

/**
 * Created by MRCan on 4/26/2016.
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.filechooser.FileSystemView;
import javax.imageio.ImageIO;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.net.URL;
public class FileBrowser {
    public static final String GUIPROJECT = "File Browser";
    private Desktop mydesktop;
    private FileSystemView myfileSystemView;
    private File currentFile;
    private JPanel mygui;
    private JTree mytree;
    private DefaultTreeModel mytreeModel;
    private JTable mytable;
    private JProgressBar progressBar;
    private FileTableModel fileTableModel;
    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private int rowIconPadding = 6;
    private JButton deleteFile;
    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;


    public Container getGui() {
        if (mygui==null) {
            mygui = new JPanel(new BorderLayout(3,3));
            mygui.setBorder(new EmptyBorder(5,5,5,5));

            myfileSystemView = FileSystemView.getFileSystemView();
            mydesktop = Desktop.getDesktop();



            JPanel detailView = new JPanel(new BorderLayout(3,3));

            mytable = new JTable();

            mytable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            mytable.setAutoCreateRowSorter(true);
            mytable.setShowVerticalLines(false);


            listSelectionListener = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent lse) {
                    int row = mytable.getSelectionModel().getLeadSelectionIndex();
                    setFileDetails( ((FileTableModel)mytable.getModel()).getFile(row) );
                }
            };
            mytable.getSelectionModel().addListSelectionListener(listSelectionListener);
            JScrollPane Scroll = new JScrollPane(mytable);
            Dimension d = Scroll.getPreferredSize();
            Scroll.setPreferredSize(new Dimension((int)d.getWidth(), (int)d.getHeight()/2));
            detailView.add(Scroll, BorderLayout.CENTER);
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            mytreeModel = new DefaultTreeModel(root);
            TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent tse){
                    DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode)tse.getPath().getLastPathComponent();
                    showChildren(node);
                    setFileDetails((File)node.getUserObject());
                }
            };
            File[] roots = myfileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add( node );
                File[] files = myfileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        node.add(new DefaultMutableTreeNode(file));
                    }
                }
            }

            mytree = new JTree(mytreeModel);
            mytree.setRootVisible(false);
            mytree.addTreeSelectionListener(treeSelectionListener);
            mytree.setCellRenderer(new FileTreeCellRenderer());
            mytree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(mytree);

            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(
                    200,
                    (int)preferredSize.getHeight());
            treeScroll.setPreferredSize( widePreferred );

            JPanel fileMainDetails = new JPanel(new BorderLayout(4,2));
            fileMainDetails.setBorder(new EmptyBorder(0,6,0,6));

            JPanel fileDetailsLabels = new JPanel(new GridLayout(0,1,2,2));

            JPanel fileDetailsValues = new JPanel(new GridLayout(0,1,2,2));
            fileMainDetails.add(fileDetailsValues, BorderLayout.SOUTH);

            fileDetailsLabels.add(new JLabel("File"));
            fileName = new JLabel();
            fileDetailsValues.add(fileName);
            fileDetailsLabels.add(new JLabel("Path/name"));
            path = new JTextField(10);
            path.setEditable(false);
            fileDetailsValues.add(path);
            fileDetailsLabels.add(new JLabel("Last Modified"));
            date = new JLabel();
            fileDetailsValues.add(date);
            fileDetailsLabels.add(new JLabel("File size"));
            size = new JLabel();
            fileDetailsValues.add(size);
            fileDetailsLabels.add(new JLabel("Type"));

            int count = fileDetailsLabels.getComponentCount();
            for (int ii=0; ii<count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }

            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);



            JButton renameFile = new JButton("Rename the File");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    renameFile();
                }
            });
            toolBar.add(renameFile);

            toolBar.addSeparator();
            deleteFile = new JButton("Delete the File");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    deleteFile();
                }
            });
            toolBar.add(deleteFile);

            toolBar.addSeparator();

            JPanel fileView = new JPanel(new BorderLayout(3,3));

            fileView.add(toolBar,BorderLayout.SOUTH);
            fileView.add(fileMainDetails,BorderLayout.EAST);

            detailView.add(fileView, BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    treeScroll,
                    detailView);
            mygui.add(splitPane, BorderLayout.CENTER);

            JPanel simpleOutput = new JPanel(new BorderLayout(3,3));
            progressBar = new JProgressBar();
            simpleOutput.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);

            mygui.add(simpleOutput, BorderLayout.SOUTH);

        }
        return mygui;
    }

    public void showRootFile() {

        mytree.setSelectionInterval(0,0);
    }

    private TreePath findTreePath(File find) {
        for (int ii=0; ii<mytree.getRowCount(); ii++) {
            TreePath treePath = mytree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)object;
            File nodeFile = (File)node.getUserObject();

            if (nodeFile==find) {
                return treePath;
            }
        }
        return null;
    }

    private void renameFile() {
        if (currentFile==null) {
            showErrorMessage("No file selected to rename.","Select File");
            return;
        }

        String renameTo = JOptionPane.showInputDialog(mygui, "Name");
        if (renameTo!=null) {
            try {
                boolean directory = currentFile.isDirectory();
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode)parentPath.getLastPathComponent();

                boolean renamed = currentFile.renameTo(new File(
                        currentFile.getParentFile(), renameTo));
                if (renamed) {
                    if (directory) {
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        mytreeModel.removeNodeFromParent(currentNode);
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                            currentFile +
                            "' could not be renamed.";
                    showErrorMessage(msg,"Rename Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        mygui.repaint();
    }

    private void deleteFile() {
        if (currentFile==null) {
            showErrorMessage("No file selected for deletion.","Select File");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                mygui,
                "Are you sure you want to delete this file?",
                "Delete File",
                JOptionPane.ERROR_MESSAGE
        );
        if (result==JOptionPane.OK_OPTION) {
            try {
                System.out.println("currentFile: " + currentFile);
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                System.out.println("parentPath: " + parentPath);
                DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                System.out.println("parentNode: " + parentNode);

                boolean directory = currentFile.isDirectory();
                boolean deleted = currentFile.delete();
                if (deleted) {
                    if (directory) {
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        mytreeModel.removeNodeFromParent(currentNode);
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                            currentFile +
                            "' could not be deleted.";
                    showErrorMessage(msg,"Delete Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        mygui.repaint();
    }

    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(
                mygui,
                errorMessage,
                errorTitle,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(
                mygui,
                t.toString(),
                t.getMessage(),
                JOptionPane.ERROR_MESSAGE
        );
        mygui.repaint();
    }

    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileTableModel==null) {
                    fileTableModel = new FileTableModel();
                    mytable.setModel(fileTableModel);
                }
                mytable.getSelectionModel().removeListSelectionListener(listSelectionListener);
                fileTableModel.setFiles(files);
                mytable.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    Icon icon = myfileSystemView.getSystemIcon(files[0]);
                    mytable.setRowHeight( icon.getIconHeight()+rowIconPadding );


                    cellSizesSet = true;
                }
            }
        });
    }
    private void showChildren(final DefaultMutableTreeNode node) {
        mytree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = myfileSystemView.getFiles(file, true); //!!
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }

            @Override
            protected void process(List<File> chunks) {
                for (File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                mytree.setEnabled(true);
            }
        };
        worker.execute();
    }
    private void setFileDetails(File file) {
        currentFile = file;
        Icon icon = myfileSystemView.getSystemIcon(file);
        fileName.setIcon(icon);
        fileName.setText(myfileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        path.setBackground(Color.pink);
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");


        JFrame f = (JFrame)mygui.getTopLevelAncestor();
        mygui.setBackground(Color.BLUE);
        if (f!=null) {
            f.setTitle(
                    myfileSystemView.getSystemDisplayName(file) );
        }

        mygui.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch(Exception weTried) {
                }
                JFrame f = new JFrame(GUIPROJECT);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                FileBrowser filebrowser = new FileBrowser();
                f.setContentPane(filebrowser.getGui());

                try {
                    URL urlBig = filebrowser.getClass().getResource("fm-icon-32x32.png");
                    URL urlSmall = filebrowser.getClass().getResource("fm-icon-16x16.png");
                    ArrayList<Image> images = new ArrayList<Image>();
                    images.add( ImageIO.read(urlBig) );
                    images.add( ImageIO.read(urlSmall) );
                    f.setIconImages(images);
                } catch(Exception weTried) {}

                f.pack();
                f.setLocationByPlatform(true);
                f.setMinimumSize(f.getSize());
                f.setVisible(true);

                filebrowser.showRootFile();

            }
        });
    }
}


