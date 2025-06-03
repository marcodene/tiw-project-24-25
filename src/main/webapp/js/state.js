const State = (() => {
    let currentUser = null;
    let playlists = []; // List of user's playlists
    let songs = []; // List of user's songs (all loaded, or per playlist)
    let currentPlaylist = null; // The currently selected/viewed playlist object
    let currentSong = null; // The currently selected/played song object
    let currentView = 'login'; // e.g., 'login', 'home', 'playlistDetail', 'player'
    let genres = []; // List of all available genres

    // Simple listener pattern for state changes (optional, can be expanded)
    const listeners = {};

    const subscribe = (event, callback) => {
        if (!listeners[event]) {
            listeners[event] = [];
        }
        listeners[event].push(callback);
    };

    const notify = (event, data) => {
        if (listeners[event]) {
            listeners[event].forEach(callback => callback(data));
        }
    };

    return {
        getCurrentUser: () => currentUser,
        setCurrentUser: (user) => {
            currentUser = user;
            notify('userChanged', currentUser);
        },

        getPlaylists: () => playlists,
        setPlaylists: (newPlaylists) => {
            playlists = newPlaylists;
            notify('playlistsChanged', playlists);
        },
        // Helper to add or update a single playlist
        updatePlaylist: (playlist) => {
            const index = playlists.findIndex(p => p.ID === playlist.ID);
            if (index > -1) {
                playlists[index] = playlist;
            } else {
                playlists.push(playlist);
            }
            notify('playlistsChanged', playlists);
            if (currentPlaylist && currentPlaylist.ID === playlist.ID) {
                State.setCurrentPlaylist(playlist); // Update current if it's the one changed
            }
        },
        removePlaylistById: (playlistId) => {
            playlists = playlists.filter(p => p.ID !== playlistId);
            notify('playlistsChanged', playlists);
            if (currentPlaylist && currentPlaylist.ID === playlistId) {
                State.setCurrentPlaylist(null);
            }
        },


        getSongs: () => songs, // This might represent all songs or songs of current playlist
        setSongs: (newSongs) => { // Used for setting all songs or songs for current playlist
            songs = newSongs;
            notify('songsChanged', songs);
        },
        // Helper to add or update a single song globally (if needed)
        updateSong: (song) => {
            const index = songs.findIndex(s => s.ID === song.ID);
            if (index > -1) {
                songs[index] = song;
            } else {
                songs.push(song);
            }
            notify('songsChanged', songs);
             // If this song is part of the current playlist, we might need to update that too
            if (currentPlaylist && currentPlaylist.songs) {
                const songInPlaylistIndex = currentPlaylist.songs.findIndex(s => s.ID === song.ID);
                if (songInPlaylistIndex > -1) {
                    currentPlaylist.songs[songInPlaylistIndex] = song;
                    notify('currentPlaylistChanged', currentPlaylist);
                }
            }
        },
        removeSongById: (songId) => {
            songs = songs.filter(s => s.ID !== songId);
            notify('songsChanged', songs);
            if (currentSong && currentSong.ID === songId) {
                State.setCurrentSong(null);
            }
            // Also remove from current playlist if it exists there
            if (currentPlaylist && currentPlaylist.songs) {
                const initialLength = currentPlaylist.songs.length;
                currentPlaylist.songs = currentPlaylist.songs.filter(s => s.ID !== songId);
                if (currentPlaylist.songs.length !== initialLength) {
                    notify('currentPlaylistChanged', currentPlaylist);
                }
            }
        },


        getCurrentPlaylist: () => currentPlaylist,
        setCurrentPlaylist: (playlist) => {
            currentPlaylist = playlist; // This playlist object should include its songs
            notify('currentPlaylistChanged', currentPlaylist);
        },

        getCurrentSong: () => currentSong,
        setCurrentSong: (song) => {
            currentSong = song;
            notify('currentSongChanged', currentSong);
        },

        getCurrentView: () => currentView,
        setCurrentView: (view) => {
            currentView = view;
            console.log("View changed to:", currentView);
            notify('viewChanged', currentView);
        },

        getGenres: () => genres,
        setGenres: (newGenres) => {
            genres = newGenres;
            notify('genresChanged', genres);
        },
        
        resetState: () => {
            currentUser = null;
            playlists = [];
            songs = [];
            currentPlaylist = null;
            currentSong = null;
            currentView = 'login';
            genres = [];
            console.log("State reset.");
            notify('stateReset');
        },

        subscribe,
        notify
    };
})();
