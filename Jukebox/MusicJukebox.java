import java.util.*;

// ===========================
// Core Domain
// ===========================
class Song {
    private final String id;
    private final String title;
    private final String artist;
    private final int durationSeconds;

    public Song(String id, String title, String artist, int durationSeconds) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.durationSeconds = durationSeconds;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getDurationSeconds() { return durationSeconds; }

    @Override
    public String toString() {
        return title + " by " + artist + " (" + durationSeconds + "s)";
    }
}

class Playlist {
    private final String name;
    private final List<Song> songs = new ArrayList<>();

    public Playlist(String name) {
        this.name = name;
    }

    public void addSong(Song song) {
        songs.add(song);
    }

    public void removeSong(Song song) {
        songs.remove(song);
    }

    public List<Song> getSongs() {
        return Collections.unmodifiableList(songs);
    }

    public String getName() {
        return name;
    }
}

class Catalog<T> {
    private final Map<String, T> items = new HashMap<>();

    public void addItem(String id, T item) {
        items.put(id, item);
    }

    public T getItem(String id) {
        return items.get(id);
    }

    public Collection<T> getAllItems() {
        return Collections.unmodifiableCollection(items.values());
    }
}

// ===========================
// Player State Pattern
// ===========================
interface PlayerState {
    void play(Player player);
    void pause(Player player);
    void stop(Player player);
}

class PlayingState implements PlayerState {
    public void play(Player player) {
        System.out.println("Already playing");
    }

    public void pause(Player player) {
        System.out.println("Pausing playback");
        player.setState(new PausedState());
    }

    public void stop(Player player) {
        System.out.println("Stopping playback");
        player.setState(new StoppedState());
    }
}

class PausedState implements PlayerState {
    public void play(Player player) {
        System.out.println("Resuming playback");
        player.setState(new PlayingState());
    }

    public void pause(Player player) {
        System.out.println("Already paused");
    }

    public void stop(Player player) {
        System.out.println("Stopping playback");
        player.setState(new StoppedState());
    }
}

class StoppedState implements PlayerState {
    public void play(Player player) {
        System.out.println("Starting playback");
        player.setState(new PlayingState());
    }

    public void pause(Player player) {
        System.out.println("Can't pause. Player is stopped.");
    }

    public void stop(Player player) {
        System.out.println("Already stopped");
    }
}

// ===========================
// Strategy Pattern
// ===========================
interface PlaybackStrategy {
    Song getNextSong(List<Song> queue, int currentIndex);
}

class NormalPlaybackStrategy implements PlaybackStrategy {
    public Song getNextSong(List<Song> queue, int currentIndex) {
        if (currentIndex + 1 < queue.size()) {
            return queue.get(currentIndex + 1);
        }
        return null;
    }
}

class ShufflePlaybackStrategy implements PlaybackStrategy {
    private final Random random = new Random();

    public Song getNextSong(List<Song> queue, int currentIndex) {
        return queue.get(random.nextInt(queue.size()));
    }
}

// ===========================
// Player
// ===========================
class Player {
    private PlayerState state;
    private List<Song> queue;
    private int currentIndex;
    private PlaybackStrategy playbackStrategy;

    public Player() {
        this.state = new StoppedState();
        this.queue = new ArrayList<>();
        this.currentIndex = -1;
        this.playbackStrategy = new NormalPlaybackStrategy();
    }

    public void setPlaybackStrategy(PlaybackStrategy strategy) {
        this.playbackStrategy = strategy;
    }

    public void loadQueue(List<Song> songs) {
        this.queue = new ArrayList<>(songs);
        this.currentIndex = -1;
    }

    public void play() {
        state.play(this);
        if (currentIndex == -1 && !queue.isEmpty()) {
            currentIndex = 0;
            System.out.println("Now playing: " + queue.get(currentIndex));
        }
    }

    public void pause() {
        state.pause(this);
    }

    public void stop() {
        state.stop(this);
        currentIndex = -1;
    }

    public void next() {
        if (playbackStrategy == null || queue.isEmpty()) {
            System.out.println("No strategy or queue is empty.");
            return;
        }
        Song nextSong = playbackStrategy.getNextSong(queue, currentIndex);
        if (nextSong != null) {
            currentIndex = queue.indexOf(nextSong);
            System.out.println("Now playing: " + nextSong);
        } else {
            System.out.println("End of playlist");
        }
    }

    public void setState(PlayerState state) {
        this.state = state;
    }
}

// ===========================
// User / Session / Payment
// ===========================
class User {
    private final String id;
    private final String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}

class UserSession {
    private final User user;
    private int credits;

    public UserSession(User user) {
        this.user = user;
        this.credits = 0;
    }

    public void addCredits(int amount) {
        credits += amount;
    }

    public boolean deductCredit() {
        if (credits > 0) {
            credits--;
            return true;
        }
        return false;
    }

    public User getUser() {
        return user;
    }

    public int getCredits() {
        return credits;
    }
}

class PaymentSystem {
    public void processPayment(UserSession session, int coins) {
        System.out.println("Processing " + coins + " coins.");
        session.addCredits(coins);
    }
}

// ===========================
// Singleton Jukebox
// ===========================
class Jukebox {
    private static Jukebox instance;
    private final Catalog<Song> songCatalog;
    private final Catalog<Playlist> playlistCatalog;
    private final Player player;
    private final PaymentSystem paymentSystem;
    private UserSession currentSession;

    private Jukebox() {
        songCatalog = new Catalog<>();
        playlistCatalog = new Catalog<>();
        player = new Player();
        paymentSystem = new PaymentSystem();
    }

    public static synchronized Jukebox getInstance() {
        if (instance == null) {
            instance = new Jukebox();
        }
        return instance;
    }

    public void startSession(User user) {
        currentSession = new UserSession(user);
        System.out.println("Session started for user: " + user.getName());
    }

    public void addCoins(int coins) {
        paymentSystem.processPayment(currentSession, coins);
    }

    public void playPlaylist(String playlistId) {
        Playlist playlist = playlistCatalog.getItem(playlistId);
        if (playlist == null) {
            System.out.println("Playlist not found.");
            return;
        }

        if (!currentSession.deductCredit()) {
            System.out.println("Not enough credits.");
            return;
        }

        player.loadQueue(playlist.getSongs());
        player.play();
    }

    public Catalog<Song> getSongCatalog() { return songCatalog; }
    public Catalog<Playlist> getPlaylistCatalog() { return playlistCatalog; }
    public Player getPlayer() { return player; }
}

// ===========================
// Main Demo
// ===========================
public class JukeboxSystem {
    public static void main(String[] args) {
        Jukebox jukebox = Jukebox.getInstance();

        // Add Songs to Catalog
        Song s1 = new Song("1", "Song A", "Artist 1", 180);
        Song s2 = new Song("2", "Song B", "Artist 2", 200);
        Song s3 = new Song("3", "Song C", "Artist 3", 150);

        jukebox.getSongCatalog().addItem(s1.getId(), s1);
        jukebox.getSongCatalog().addItem(s2.getId(), s2);
        jukebox.getSongCatalog().addItem(s3.getId(), s3);

        // Create Playlist
        Playlist p1 = new Playlist("Chill Vibes");
        p1.addSong(s1);
        p1.addSong(s2);
        p1.addSong(s3);
        jukebox.getPlaylistCatalog().addItem("chill", p1);

        // Start User Session
        User user = new User("u1", "John");
        jukebox.startSession(user);
        jukebox.addCoins(5);

        // Play
        jukebox.playPlaylist("chill");
        jukebox.getPlayer().next();
        jukebox.getPlayer().pause();
        jukebox.getPlayer().play();
        jukebox.getPlayer().stop();
    }
}
