import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Class to represent a note
class Note implements Serializable {
    private static final long serialVersionUID = 1L; // Add serialVersionUID for serialization
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private List<String> tags;
    private String category; // Added category

    public Note(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.creationDate = LocalDateTime.now();
        this.tags = new ArrayList<>();
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

     public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Title: " + title + "\nCategory: " + category + "\nCreated: " + creationDate.format(formatter) + "\nTags: " + tags + "\nContent:\n" + content + "\n";
    }
}

// Class to manage the collection of notes
class NoteManager implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<Note> notes;
    private transient JTextPane displayArea;  // Use transient to prevent serialization issues with GUI components
    private transient JComboBox<String> categoryComboBox; // Transient for ComboBox
    private static final String DATA_FILE = "notes.dat";

    public NoteManager() {
        this.notes = new ArrayList<>();
        this.displayArea = new JTextPane();
        this.categoryComboBox = new JComboBox<>();
        loadNotes(); // Load notes from file when NoteManager is initialized
    }

    // Method to set the display area
    public void setDisplayArea(JTextPane displayArea) {
        this.displayArea = displayArea;
        // Set the content type to HTML to enable rich text formatting
        this.displayArea.setContentType("text/html");
        //apply css
        setStyleSheet();
    }

     // Method to set the category ComboBox
    public void setCategoryComboBox(JComboBox<String> categoryComboBox) {
        this.categoryComboBox = categoryComboBox;
        loadCategories();
    }

    // Method to add a new note
    public void addNote(String title, String content, String category) {
        Note newNote = new Note(title, content, category);
        notes.add(newNote);
        saveNotes(); // Save notes to file after adding
        loadCategories(); //update categories
    }

    // Method to edit an existing note
    public void editNote(int index, String newTitle, String newContent, String newCategory) {
        if (index >= 0 && index < notes.size()) {
            Note note = notes.get(index);
            note.setTitle(newTitle);
            note.setContent(newContent);
            note.setCategory(newCategory); //update category
            saveNotes();  // Save changes to file
            loadCategories();
        } else {
            displayArea.setText("Invalid note index.");
        }
    }

    // Method to delete a note
    public void deleteNote(int index) {
        if (index >= 0 && index < notes.size()) {
            notes.remove(index);
            saveNotes(); // Save changes to file
            loadCategories();
        } else {
            displayArea.setText("Invalid note index.");
        }
    }

    // Method to display a single note
    public void displayNote(int index) {
        if (index >= 0 && index < notes.size()) {
            Note note = notes.get(index);
            displayArea.setText(generateHTMLContent(note)); //use html to display
        } else {
            displayArea.setText("Invalid note index.");
        }
    }

    // Method to display all notes
    public void displayAllNotes() {
        if (notes.isEmpty()) {
            displayArea.setText("No notes available.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                sb.append(generateHTMLContent(note)); //use html here too
                sb.append("<hr>"); // Add a horizontal line between notes
            }
            displayArea.setText(sb.toString());
        }
    }

    // Method to search notes by title or content
   public List<Note> searchNotes(String query) {
        List<Note> matchingNotes = new ArrayList<>();
        for (Note note : notes) {
            if (note.getTitle().toLowerCase().contains(query.toLowerCase())
                    || note.getContent().toLowerCase().contains(query.toLowerCase())) {
                matchingNotes.add(note);
            }
        }
        return matchingNotes;
    }

    // Method to add a tag to a note
    public void addTagToNote(int index, String tag) {
        if (index >= 0 && index < notes.size()) {
            notes.get(index).addTag(tag);
            saveNotes(); // Save changes
        } else {
            displayArea.setText("Invalid note index.");
        }
    }

    // Method to remove a tag from a note
    public void removeTagFromNote(int index, String tag) {
        if (index >= 0 && index < notes.size()) {
            notes.get(index).removeTag(tag);
            saveNotes(); // Save changes
        } else {
            displayArea.setText("Invalid note index.");
        }
    }

    // Method to display notes with a specific tag
    public void displayNotesByTag(String tag) {
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for (Note note : notes) {
            if (note.getTags().contains(tag)) {
                sb.append(generateHTMLContent(note));
                sb.append("<hr>");
                found = true;
            }
        }
        if (!found) {
            displayArea.setText("No notes found with tag: " + tag);
        } else {
            displayArea.setText(sb.toString());
        }
    }

     // Method to display notes within a category
    public void displayNotesByCategory(String category) {
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for (Note note : notes) {
            if (note.getCategory().equalsIgnoreCase(category)) {
                sb.append(generateHTMLContent(note));
                sb.append("<hr>");
                found = true;
            }
        }
        if (!found) {
            displayArea.setText("No notes found in category: " + category);
        } else {
            displayArea.setText(sb.toString());
        }
    }

    // Method to get all unique categories
    public List<String> getAllCategories() {
        Set<String> categories = new HashSet<>();
        for (Note note : notes) {
            categories.add(note.getCategory());
        }
        return new ArrayList<>(categories);
    }

    // Method to load categories into the ComboBox
    private void loadCategories() {
        categoryComboBox.removeAllItems(); // Clear existing items
        List<String> categories = getAllCategories();
        for (String category : categories) {
            categoryComboBox.addItem(category);
        }
    }

    // Method to save notes to a file
    private void saveNotes() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(notes);
        } catch (IOException e) {
            Logger.getLogger(NoteManager.class.getName()).log(Level.SEVERE, "Error saving notes: ", e);
            displayArea.setText("Error saving notes to file.");
        }
    }

    // Method to load notes from a file
    @SuppressWarnings("unchecked") // Suppress unchecked cast warning
    private void loadNotes() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
                notes = (List<Note>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                Logger.getLogger(NoteManager.class.getName()).log(Level.SEVERE, "Error loading notes: ", e);
                displayArea.setText("Error loading notes from file. Creating a new note list.");
                notes = new ArrayList<>(); // Initialize to avoid NullPointerException
            }        } else {
             System.out.println("No existing note file found. Starting with an empty note list.");
             notes = new ArrayList<>();
        }
    }

    //helper method to generate html
    private String generateHTMLContent(Note note){
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; margin: 10px;'>");
        sb.append("<h1 style='color: #333;'>").append(note.getTitle()).append("</h1>");
        sb.append("<p style='color: #666;'>Category: <span style='font-weight: bold;'>").append(note.getCategory()).append("</span></p>");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        sb.append("<p style='color: #888;'>Created: ").append(note.getCreationDate().format(formatter)).append("</p>");
        sb.append("<p style='color: #666;'>Tags: ");
        if (note.getTags().isEmpty()) {
            sb.append("<span style='font-style: italic;'>No tags</span>");
        } else {
            for (String tag : note.getTags()) {
                sb.append("<span style='background-color: #e0e0e0; padding: 3px 6px; border-radius: 5px; margin-right: 5px;'>").append(tag).append("</span>");
            }
        }
        sb.append("</p>");
        // Use a JEditorPane for content, so html tags are interpreted.
        String content = note.getContent();

        // Basic HTML formatting for the content (paragraphs, line breaks, etc.)
        content = content.replace("\n", "<br>");

        sb.append("<div style='margin-top: 10px; line-height: 1.5;'>").append(content).append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void setStyleSheet() {
        // Create a new style sheet.
        StyleSheet styleSheet = new StyleSheet();

        // Add CSS rules to the style sheet.  These could also be loaded from a file.
        styleSheet.addRule("body {font-family: Arial, sans-serif; margin: 10px;}");
        styleSheet.addRule("h1 {color: #333;}");
        styleSheet.addRule("p {color: #666;}");
        styleSheet.addRule("pre {background-color: #f0f0f0; padding: 10px; border: 1px solid #ccc; overflow-x: auto;}");
        styleSheet.addRule("code {font-family: monospace; background-color: #f0f0f0; padding: 2px 5px; border-radius: 5px;}");
        styleSheet.addRule(".tag {background-color: #e0e0e0; padding: 3px 6px; border-radius: 5px; margin-right: 5px;}");

        // Get the editor kit for the text pane.
        HTMLEditorKit kit = (HTMLEditorKit) displayArea.getEditorKit();

        // Add the style sheet to the editor kit.
        kit.setStyleSheet(styleSheet);
    }
}

// Main class for the application
public class AdvancedNoteTaker {

    private static NoteManager noteManager;
    private static JFrame frame;
    private static JTextArea inputArea;
    private static JTextPane displayArea;
    private static JTextField titleField;
    private static JTextField tagField;
    private static JTextField searchField;
    private static JComboBox<String> categoryComboBox; // Added ComboBox
    private static int currentNoteIndex = -1; // To keep track of the currently displayed note

    public static void main(String[] args) {
        // Set up the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (Exception e) {
                Logger.getLogger(AdvancedNoteTaker.class.getName()).log(Level.SEVERE, "Exception during GUI initialization: ", e);
                JOptionPane.showMessageDialog(null,
                        "Failed to initialize the application. Please check the logs for details.",
                        "Initialization Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Method to create and show the GUI
    private static void createAndShowGUI() {
        frame = new JFrame("Advanced Note Taker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null); // Center the frame

        // Initialize the NoteManager
        noteManager = new NoteManager();

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Top panel for buttons and input fields
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Title field
        titleField = new JTextField(20);
        titleField.setToolTipText("Enter note title");
        topPanel.add(new JLabel("Title:"));
        topPanel.add(titleField);

        // Category ComboBox
        categoryComboBox = new JComboBox<>();
        categoryComboBox.setToolTipText("Select note category");
        topPanel.add(new JLabel("Category:"));
        topPanel.add(categoryComboBox);
        noteManager.setCategoryComboBox(categoryComboBox); //set the combobox in note manager

        // Input area for note content
        inputArea = new JTextArea(10, 40);
        inputArea.setWrapStyleWord(true);
        inputArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(inputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Note Content"));

        // Display area for formatted notes
        displayArea = new JTextPane();
        displayArea.setEditable(false);
        displayArea.setContentType("text/html"); // Set content type to HTML
        noteManager.setDisplayArea(displayArea); // Pass the displayArea to the NoteManager
        JScrollPane displayScrollPane = new JScrollPane(displayArea);
        displayScrollPane.setBorder(BorderFactory.createTitledBorder("View Notes"));

        // Tag field
        tagField = new JTextField(10);
        tagField.setToolTipText("Enter tags separated by commas");
        topPanel.add(new JLabel("Tags:"));
        topPanel.add(tagField);

        // Search field
        searchField = new JTextField(15);
        searchField.setToolTipText("Enter search query");
        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);

        // Buttons
        JButton addButton = new JButton("Add Note");
        JButton editButton = new JButton("Edit Note");
        JButton deleteButton = new JButton("Delete Note");
        JButton viewButton = new JButton("View Note");
        JButton viewAllButton = new JButton("View All Notes");
        JButton searchButton = new JButton("Search");
        JButton addTagButton = new JButton("Add Tag");
        JButton removeTagButton = new JButton("Remove Tag");
        JButton viewByCategoryButton = new JButton("View by Category");

        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        topPanel.add(viewButton);
        topPanel.add(viewAllButton);
        topPanel.add(searchButton);
        topPanel.add(addTagButton);
        topPanel.add(removeTagButton);
        topPanel.add(viewByCategoryButton);

        // Add components to the main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(displayScrollPane, BorderLayout.SOUTH);

        // Set the main panel as the content pane
        frame.setContentPane(mainPanel);

        // Add action listeners to the buttons
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = titleField.getText().trim();
                String content = inputArea.getText().trim();
                String category = (String) categoryComboBox.getSelectedItem(); // Get selected category

                if (title.isEmpty() || content.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter both title and content.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                noteManager.addNote(title, content, category);
                clearInputFields();
                displayArea.setText("Note added successfully.");
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentNoteIndex == -1) {
                    JOptionPane.showMessageDialog(frame, "Please select a note to edit by viewing it first.", "No Note Selected", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String newTitle = titleField.getText().trim();
                String newContent = inputArea.getText().trim();
                 String newCategory = (String) categoryComboBox.getSelectedItem();

                if (newTitle.isEmpty() || newContent.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter both title and content.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                noteManager.editNote(currentNoteIndex, newTitle, newContent, newCategory);
                clearInputFields();
                displayArea.setText("Note edited successfully.");
                currentNoteIndex = -1; // Reset
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentNoteIndex == -1) {
                    JOptionPane.showMessageDialog(frame, "Please select a note to delete by viewing it first.", "No Note Selected", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int choice = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this note?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    noteManager.deleteNote(currentNoteIndex);
                    clearInputFields();
                    displayArea.setText("Note deleted successfully.");
                    currentNoteIndex = -1; // Reset
                }
            }
        });

        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter the title of the note to view.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                boolean found = false;
                for (int i = 0; i < noteManager.searchNotes(title).size(); i++) {
                    if (noteManager.searchNotes(title).get(i).getTitle().equalsIgnoreCase(title)) {
                        noteManager.displayNote(i);
                        currentNoteIndex = i; // Store the index of the displayed note
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    JOptionPane.showMessageDialog(frame, "Note not found.", "Note Not Found", JOptionPane.INFORMATION_MESSAGE);
                    currentNoteIndex = -1;
                }
            }
        });

        viewAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                noteManager.displayAllNotes();
                currentNoteIndex = -1; // Reset
            }
        });

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String query = searchField.getText().trim();
                if (query.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a search query.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                List<Note> results = noteManager.searchNotes(query);
                if (results.isEmpty()) {
                    displayArea.setText("No notes found matching the query.");
                    currentNoteIndex = -1;
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Note note : results) {
                        sb.append(generateHTMLContent(note));
                        sb.append("<hr>");
                    }
                    displayArea.setText(sb.toString());
                    currentNoteIndex = -1;
                }
            }
        });

        addTagButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentNoteIndex == -1) {
                    JOptionPane.showMessageDialog(frame, "Please select a note to add a tag to by viewing it first.", "No Note Selected", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String tag = tagField.getText().trim();
                if (tag.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a tag to add.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                noteManager.addTagToNote(currentNoteIndex, tag);
                displayNote(currentNoteIndex); //refresh display
                tagField.setText("");
            }
        });

        removeTagButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentNoteIndex == -1) {
                    JOptionPane.showMessageDialog(frame, "Please select a note to remove a tag from by viewing it first.", "No Note Selected", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String tag = tagField.getText().trim();
                if (tag.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a tag to remove.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                noteManager.removeTagFromNote(currentNoteIndex, tag);
                displayNote(currentNoteIndex); //refresh
                tagField.setText("");
            }
        });

        viewByCategoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                 String category = (String) categoryComboBox.getSelectedItem();
                  if (category == null || category.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please select a category to view.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                noteManager.displayNotesByCategory(category);
                currentNoteIndex = -1;
            }
        });

        // Show the frame
        frame.setVisible(true);
    }

    // Method to clear input fields
    private static void clearInputFields() {
        titleField.setText("");
        inputArea.setText("");
        tagField.setText("");
        searchField.setText("");
    }
}

