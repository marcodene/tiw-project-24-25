const State = (() => {
    let currentUser = SessionManager.getUser(); // Initialize from SessionManager
    let playlists = []; // List of user's playlists
    let songs = []; // List of user's songs (all loaded, or per playlist)
    let currentPlaylist = null; // The currently selected/viewed playlist object
    let currentSong = null; // The currently selected/played song object
    let currentView = 'login'; // e.g., 'login', 'home', 'playlistDetail', 'player'
    let genres = []; // List of all available genres

    // Simple listener pattern for state changes (optional, can be expanded)
    const listeners = {};

    /**
     * Subscribes a callback function to a specific state change event
     * Called by components that need to react to state changes (e.g., Home.render, PlaylistComponent)
     * Allows components to update when relevant data changes
     */
    const subscribe = (event, callback) => {
        if (!listeners[event]) {
            listeners[event] = [];
        }
        listeners[event].push(callback);
    };

    /**
     * Notifies all subscribers of a specific state change event
     * Called internally by state setter methods when data changes
     * Triggers callbacks registered via subscribe() for the given event
     */
    const notify = (event, data) => {
        if (listeners[event]) {
            listeners[event].forEach(callback => callback(data));
        }
    };

    return {
        /**
         * Gets the current authenticated user data
         * Called throughout the app to access user information
         * Always syncs with SessionManager to ensure consistency
         */
        getCurrentUser: () => {
            // Always sync with SessionManager
            const stored = SessionManager.getUser();
            if (stored && (!currentUser || currentUser.id !== stored.id)) {
                currentUser = stored;
            }
            return currentUser;
        },
		
        /**
         * Sets the current authenticated user data
         * Called by Auth module during login/registration and App.init()
         * Updates both local state and SessionManager, then notifies subscribers
         */
        setCurrentUser: (user) => {
            currentUser = user;
            SessionManager.setUser(user); // Keep SessionManager in sync
            notify('userChanged', currentUser);
        },

        /**
         * Gets the list of user's playlists
         * Called by Home component to display playlist list
         * Returns array of playlist objects
         */
        getPlaylists: () => playlists,
		
        /**
         * Sets the complete list of user's playlists
         * Called by App.fetchInitialData() after loading playlists from server
         * Replaces entire playlist array and notifies subscribers
         */
        setPlaylists: (newPlaylists) => {
            playlists = newPlaylists;
            notify('playlistsChanged', playlists);
        },
        // Helper to add or update a single playlist
        /**
         * Adds or updates a single playlist in the state
         * Called when creating new playlists or updating existing ones
         * Updates the playlist array and current playlist if it matches
         */
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
        /**
         * Removes a playlist from state by ID
         * Called when deleting playlists from Home component
         * Filters out the playlist and updates current playlist if needed
         */
        removePlaylistById: (playlistId) => {
            playlists = playlists.filter(p => p.ID !== playlistId);
            notify('playlistsChanged', playlists);
            if (currentPlaylist && currentPlaylist.ID === playlistId) {
                State.setCurrentPlaylist(null);
            }
        },


        /**
         * Gets the list of user's songs
         * Called by Home component for creating playlists and displaying songs
         * Returns array of song objects (all user songs or current playlist songs)
         */
        getSongs: () => songs, // This might represent all songs or songs of current playlist
        /**
         * Sets the complete list of songs
         * Called by App.fetchInitialData() after loading songs from server
         * Replaces entire songs array and notifies subscribers
         */
        setSongs: (newSongs) => { // Used for setting all songs or songs for current playlist
            songs = newSongs;
            notify('songsChanged', songs);
        },
        // Helper to add or update a single song globally (if needed)
        /**
         * Adds or updates a single song in the global state
         * Called when uploading new songs or updating existing ones
         * Updates both global songs list and current playlist if song is in it
         */
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
        /**
         * Removes a song from state by ID
         * Called when deleting songs from PlayerComponent
         * Removes from global songs, current song, and current playlist
         */
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


        /**
         * Gets the currently selected/viewed playlist
         * Called by components to access current playlist data
         * Returns playlist object with songs array
         */
        getCurrentPlaylist: () => currentPlaylist,
		
        /**
         * Sets the currently selected/viewed playlist
         * Called when navigating to playlist view or updating playlist data
         * Updates current playlist and notifies subscribers
         */
        setCurrentPlaylist: (playlist) => {
            currentPlaylist = playlist; // This playlist object should include its songs
            notify('currentPlaylistChanged', currentPlaylist);
        },

        /**
         * Gets the currently selected/playing song
         * Called by PlayerComponent to access song data
         * Returns song object with metadata
         */
        getCurrentSong: () => currentSong,
		
        /**
         * Sets the currently selected/playing song
         * Called when navigating to player view or selecting a song
         * Updates current song and notifies subscribers
         */
        setCurrentSong: (song) => {
            currentSong = song;
            notify('currentSongChanged', currentSong);
        },

        /**
         * Gets the current application view/route
         * Called by router and components to check current view state
         * Returns string representing current view (e.g., 'home', 'playlist', 'player')
         */
        getCurrentView: () => currentView,
		
        /**
         * Sets the current application view/route
         * Called by Router.navigateTo() when changing views
         * Updates view state and notifies subscribers
         */
        setCurrentView: (view) => {
            currentView = view;
            console.log("View changed to:", currentView);
            notify('viewChanged', currentView);
        },

        /**
         * Gets the list of available music genres
         * Called by Home component to populate genre dropdown in upload form
         * Returns array of genre strings
         */
        getGenres: () => genres,
		
        /**
         * Sets the list of available music genres
         * Called by App.fetchInitialData() after loading genres from server
         * Updates genres array and notifies subscribers
         */
        setGenres: (newGenres) => {
            genres = newGenres;
            notify('genresChanged', genres);
        },
        
        /**
         * Completely resets all application state to initial values
         * Called by App.reset() during logout process
         * Clears all user data and session information
         */
        resetState: () => {
            currentUser = null;
            playlists = [];
            songs = [];
            currentPlaylist = null;
            currentSong = null;
            currentView = 'login';
            genres = [];
            SessionManager.clearUser(); // Clear SessionManager too
            console.log("State reset.");
            notify('stateReset');
        },

        subscribe,
        notify
    };
})();
