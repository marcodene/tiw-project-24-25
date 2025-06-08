-- Creazione del database
CREATE DATABASE IF NOT EXISTS db_progetto2425;
USE db_progetto2425;

-- Drop delle tabelle in ordine inverso rispetto alle dipendenze (per evitare errori di chiavi esterne)
DROP TABLE IF EXISTS PlaylistSong;
DROP TABLE IF EXISTS Playlist;
DROP TABLE IF EXISTS Song;
DROP TABLE IF EXISTS Genre;
DROP TABLE IF EXISTS User;

-- Creazione della tabella User
CREATE TABLE IF NOT EXISTS User (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL
);

-- Creazione della tabella Genre
CREATE TABLE IF NOT EXISTS Genre (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Creazione della tabella Song
CREATE TABLE IF NOT EXISTS Song (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    userID INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    genreID INT NOT NULL,
    file VARCHAR(255) NOT NULL,
    albumCover VARCHAR(255) NOT NULL,
    albumName VARCHAR(100) NOT NULL,
    albumArtist VARCHAR(100) NOT NULL,
    albumReleaseYear INT NOT NULL,
    FOREIGN KEY (userID) REFERENCES User(ID) ON DELETE CASCADE,
    FOREIGN KEY (genreID) REFERENCES Genre(ID) ON DELETE RESTRICT -- Keep RESTRICT if genres are fixed and essential
);

-- Creazione della tabella Playlist
CREATE TABLE IF NOT EXISTS Playlist (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creationDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    userID INT NOT NULL,
    FOREIGN KEY (userID) REFERENCES User(ID) ON DELETE CASCADE
);

-- Creazione della tabella di associazione PlaylistSong
CREATE TABLE IF NOT EXISTS PlaylistSong (
    playlistID INT NOT NULL,
    songID INT NOT NULL,
    customOrder INT, -- Added for custom playlist ordering
    PRIMARY KEY (playlistID, songID),
    FOREIGN KEY (playlistID) REFERENCES Playlist(ID) ON DELETE CASCADE,
    FOREIGN KEY (songID) REFERENCES Song(ID) ON DELETE CASCADE
);

-- Indici per migliorare le performance
CREATE INDEX idx_song_name ON Song(name);
CREATE INDEX idx_song_album ON Song(albumName, albumArtist);
CREATE INDEX idx_playlist_name ON Playlist(name);
CREATE INDEX idx_playlist_creation ON Playlist(creationDate);
CREATE INDEX idx_playlistsong_customorder ON PlaylistSong(playlistID, customOrder); -- Index for ordered fetching

-- Inserimento utente con blocco tabella
LOCK TABLES `User` WRITE;
/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO User (username, password, name, surname)
VALUES
('user1', 'user1', 'Marco', 'De Negri'),
('user2', 'user2', 'Alice', 'Berta');
/*!40000 ALTER TABLE `User` ENABLE KEYS */;
UNLOCK TABLES;

-- Inserimento generi musicali con blocco tabella
LOCK TABLES `Genre` WRITE;
/*!40000 ALTER TABLE `Genre` DISABLE KEYS */;
INSERT INTO Genre (name) VALUES 
('Rock'),
('Pop'),
('Jazz'),
('Hip Hop'),
('Classical'),
('Electronic'),
('R&B'),
('Country'),
('Reggae'),
('Metal');
/*!40000 ALTER TABLE `Genre` ENABLE KEYS */;
UNLOCK TABLES;
