import java.io.InputStream;
import java.util.*;

public class EBookReaderSystem {

    /* ---------- MODELS ---------- */

    public static class Page {
        private final int pageNumber;
        private final String content;

        public Page(int pageNumber, String content) {
            this.pageNumber = pageNumber;
            this.content = content;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public String getContent() {
            return content;
        }
    }

    public static class Annotation {
        private final int pageNumber;
        private final String note;
        private final String highlightedText;
        private final Date timestamp;

        public Annotation(int pageNumber, String note, String highlightedText) {
            this.pageNumber = pageNumber;
            this.note = note;
            this.highlightedText = highlightedText;
            this.timestamp = new Date();
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public String getNote() {
            return note;
        }

        public String getHighlightedText() {
            return highlightedText;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    public enum BookFormat {
        PDF,
        EPUB
    }

    public static class Book {
        private final String id;
        private final String title;
        private final String author;
        private final BookFormat format;
        private final List<Page> pages;
        private final List<Annotation> annotations;

        public Book(String id, String title, String author, BookFormat format,
                    List<Page> pages, List<Annotation> annotations) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.format = format;
            this.pages = pages;
            this.annotations = annotations;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        public BookFormat getFormat() {
            return format;
        }

        public List<Page> getPages() {
            return pages;
        }

        public List<Annotation> getAnnotations() {
            return annotations;
        }
    }

    public static class ReadingProgress {
        private final String bookId;
        private final int lastReadPage;

        public ReadingProgress(String bookId, int lastReadPage) {
            this.bookId = bookId;
            this.lastReadPage = lastReadPage;
        }

        public String getBookId() {
            return bookId;
        }

        public int getLastReadPage() {
            return lastReadPage;
        }
    }

    public static class User {
        private final String userId;
        private final String name;
        private final Map<String, ReadingProgress> progressMap = new HashMap<>();

        public User(String userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public ReadingProgress getProgress(String bookId) {
            return progressMap.getOrDefault(bookId, new ReadingProgress(bookId, 0));
        }

        public void updateProgress(String bookId, int pageNumber) {
            progressMap.put(bookId, new ReadingProgress(bookId, pageNumber));
        }

        public Map<String, ReadingProgress> getAllProgress() {
            return Collections.unmodifiableMap(progressMap);
        }

        public String getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }
    }

    public static class Library {
        private final Map<String, Book> books = new HashMap<>();

        public void addBook(Book book) {
            books.put(book.getId(), book);
        }

        public void removeBook(String id) {
            books.remove(id);
        }

        public Book getBook(String id) {
            return books.get(id);
        }

        public Collection<Book> getAllBooks() {
            return books.values();
        }
    }

    /* ---------- PARSING ---------- */

    public interface BookParser {
        List<Page> parse(InputStream input) throws Exception;
    }

    public static class PDFBookParser implements BookParser {
        @Override
        public List<Page> parse(InputStream input) {
            // Mock parsing: in real world, parse actual PDF
            List<Page> pages = new ArrayList<>();
            pages.add(new Page(0, "PDF Page 1"));
            pages.add(new Page(1, "PDF Page 2"));
            return pages;
        }
    }

    public static class EPUBBookParser implements BookParser {
        @Override
        public List<Page> parse(InputStream input) {
            // Mock parsing: in real world, parse actual EPUB
            List<Page> pages = new ArrayList<>();
            pages.add(new Page(0, "EPUB Page 1"));
            pages.add(new Page(1, "EPUB Page 2"));
            return pages;
        }
    }

    public static class BookParserFactory {
        public static BookParser getParser(BookFormat format) {
            return switch (format) {
                case PDF -> new PDFBookParser();
                case EPUB -> new EPUBBookParser();
                default -> throw new UnsupportedOperationException("Unsupported format");
            };
        }
    }

    /* ---------- READER ---------- */

    public static class Reader {
        private Book currentBook;
        private int currentPageNumber;
        private User currentUser;

        public void openBook(Book book, User user) {
            this.currentBook = book;
            this.currentUser = user;
            this.currentPageNumber = user.getProgress(book.getId()).getLastReadPage();
        }

        public Page getCurrentPage() {
            return currentBook.getPages().get(currentPageNumber);
        }

        public void nextPage() {
            if (currentPageNumber < currentBook.getPages().size() - 1) {
                currentPageNumber++;
            }
        }

        public void prevPage() {
            if (currentPageNumber > 0) {
                currentPageNumber--;
            }
        }

        public void saveProgress() {
            currentUser.updateProgress(currentBook.getId(), currentPageNumber);
        }

        public void addAnnotation(Annotation annotation) {
            currentBook.getAnnotations().add(annotation);
        }
    }

    /* ---------- SEARCH ---------- */

    public interface SearchStrategy {
        List<Integer> search(Book book, String query);
    }

    public static class SimpleTextSearchStrategy implements SearchStrategy {
        @Override
        public List<Integer> search(Book book, String query) {
            List<Integer> resultPages = new ArrayList<>();
            for (Page page : book.getPages()) {
                if (page.getContent().contains(query)) {
                    resultPages.add(page.getPageNumber());
                }
            }
            return resultPages;
        }
    }

    /* ---------- USER SETTINGS ---------- */

    public static class UserSettings {
        private String font;
        private int fontSize;
        private String theme;

        public UserSettings(String font, int fontSize, String theme) {
            this.font = font;
            this.fontSize = fontSize;
            this.theme = theme;
        }

        public String getFont() {
            return font;
        }

        public void setFont(String font) {
            this.font = font;
        }

        public int getFontSize() {
            return fontSize;
        }

        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }
    }

    /* ---------- MAIN (DEMO) ---------- */

    public static void main(String[] args) throws Exception {
        // Create library and user
        Library library = new Library();
        User user = new User("u1", "Shubham");

        // Load book (simulate parse)
        BookParser parser = BookParserFactory.getParser(BookFormat.PDF);
        List<Page> pages = parser.parse(new InputStream() {
            @Override
            public int read() {
                return -1;
            }
        });

        Book book = new Book("b1", "Design Patterns", "Gang of Four", BookFormat.PDF, pages, new ArrayList<>());
        library.addBook(book);

        // Open and read
        Reader reader = new Reader();
        reader.openBook(book, user);
        System.out.println("Opened at page: " + reader.getCurrentPage().getPageNumber());

        reader.nextPage();
        reader.saveProgress();

        // Simulate closing and reopening
        Reader newReader = new Reader();
        newReader.openBook(book, user);
        System.out.println("Resumed at page: " + newReader.getCurrentPage().getPageNumber());

        // Search
        SearchStrategy search = new SimpleTextSearchStrategy();
        List<Integer> results = search.search(book, "PDF");
        System.out.println("Search results: " + results);
    }
}
