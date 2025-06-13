const Home = (() => {
    let container; // To store the main container element for the home view
	
    /**
     * Renders the main home page interface
     * Called by Router when navigating to 'home' route
     * Creates welcome message, playlists section, create playlist form, and upload song form
     */
    const render = (appContainer) => {
        container = appContainer;
        container.innerHTML = ''; // Clear previous content

        const welcomeMessage = document.createElement('h2');
        const currentUser = State.getCurrentUser();
        welcomeMessage.textContent = currentUser ? `Welcome to your Music Dashboard, ${currentUser.name}!` : 'Home';
        container.appendChild(welcomeMessage);

        // Sections
        const playlistsSection = document.createElement('section');
        playlistsSection.id = 'home-playlists-section';
        container.appendChild(playlistsSection);

        const formsContainer = document.createElement('div');
        formsContainer.className = 'form-columns';
        
        const createPlaylistSection = document.createElement('section');
        createPlaylistSection.id = 'home-create-playlist-section';
        createPlaylistSection.className = 'card';
        formsContainer.appendChild(createPlaylistSection);
        
        const uploadSongSection = document.createElement('section');
        uploadSongSection.id = 'home-upload-song-section';
        uploadSongSection.className = 'card';
        formsContainer.appendChild(uploadSongSection);
        
        container.appendChild(formsContainer);

        renderPlaylists(playlistsSection);
        renderCreatePlaylistForm(createPlaylistSection);
        renderUploadSongForm(uploadSongSection);

        // Subscribe to state changes to re-render if necessary
        State.subscribe('playlistsChanged', () => renderPlaylists(playlistsSection));
        State.subscribe('songsChanged', () => renderCreatePlaylistForm(createPlaylistSection));
        State.subscribe('genresChanged', () => renderUploadSongForm(uploadSongSection));
    };

    /**
     * Renders the user's playlists as a clickable list
     * Called by render() and when playlists change via state subscription
     * Creates playlist items with view, reorder, and delete buttons
     */
    const renderPlaylists = (sectionElement) => {
        sectionElement.innerHTML = '<h3>Your Playlists</h3>';
        const playlists = State.getPlaylists();
        if (!playlists || playlists.length === 0) {
            sectionElement.innerHTML += '<p>You have no playlists yet. Create one below!</p>';
            return;
        }

        const grid = document.createElement('div');
        grid.className = 'playlist-grid';
        playlists.forEach(playlist => {
            const card = document.createElement('div');
            card.className = 'playlist-card';
            
            card.innerHTML = SecurityUtils.createSafeHTML(`
                <h4>{{name}}</h4>
                <p>{{creationDate}}</p>
                <div class="playlist-actions">
                    <button class="view-btn">View</button>
                    <button class="delete-btn">Delete</button>
                </div>
            `, {
                name: playlist.name,
                creationDate: playlist.creationDate || ""
            });

            // Add click event to view button
            const viewButton = card.querySelector('.view-btn');
            viewButton.addEventListener('click', (e) => {
				e.stopPropagation();
				State.setCurrentPlaylist(playlist);
                Router.navigateTo('playlist', playlist);
            });
            
            // Add click event to delete button
            const deleteButton = card.querySelector('.delete-btn');
            deleteButton.addEventListener('click', (e) => {
                e.stopPropagation();
                handleDeletePlaylist(playlist.ID);
            });

            grid.appendChild(card);
        });
        sectionElement.appendChild(grid);
    };

    /**
     * Renders the song upload form with file inputs and genre selection
     * Called by render() and when genres change via state subscription
     * Creates form for uploading audio files with metadata
     */
    const renderUploadSongForm = (sectionElement) => {
        sectionElement.innerHTML = '<h3>Upload New Song</h3>';
        const genres = State.getGenres();

        const form = document.createElement('form');
        form.id = 'uploadSongForm';
        form.innerHTML = `
            <div><label for="songTitle">Title:</label><input type="text" id="songTitle" name="title" required></div>
            <div><label for="songAlbum">Album:</label><input type="text" id="songAlbum" name="albumName" required></div>
            <div><label for="songArtist">Artist:</label><input type="text" id="songArtist" name="artistName" required></div>
            <div><label for="songYear">Year:</label><input type="number" id="songYear" name="albumReleaseYear" required></div>
            <div>
                <label for="songGenre">Genre:</label>
                <select id="songGenre" name="genreName" required>
                    <option value="">Select Genre</option>
                    ${genres.map(genre => `<option value="${genre}">${genre}</option>`).join('')}
                </select>
            </div>
            <div><label for="songAudioFile">Audio File (MP3, WAV):</label><input type="file" id="songAudioFile" name="audioFile" accept=".mp3,.wav" required></div>
            <div><label for="songImageFile">Cover Image (JPG, PNG):</label><input type="file" id="songImageFile" name="imageFile" accept=".jpg,.jpeg,.png" required></div>
            <button type="submit">Upload Song</button>
            <div id="uploadSongMessage" class="message-area"></div>
        `;
        form.addEventListener('submit', handleUploadSong);
        sectionElement.appendChild(form);
    };

    /**
     * Renders the playlist creation form with song selection checkboxes
     * Called by render() and when songs change via state subscription
     * Creates form for selecting existing songs to add to new playlist
     */
    const renderCreatePlaylistForm = (sectionElement) => {
        sectionElement.innerHTML = '<h3>Create New Playlist</h3>';
        const allSongs = State.getSongs(); // Assuming all user songs are loaded in State

        const form = document.createElement('form');
        form.id = 'createPlaylistForm';
        let songsHtml = '<p>No songs available to add. Upload songs first.</p>';
        if (allSongs && allSongs.length > 0) {
            songsHtml = allSongs.map(song => SecurityUtils.createSafeHTML(`
                <div>
                    <input type="checkbox" id="song-{{songId}}" name="selectedSongs" value="{{songId}}">
                    <label for="song-{{songId}}">{{name}} - {{artistName}} ({{albumName}}, {{albumReleaseYear}})</label>
                </div>
            `, {
                songId: song.ID,
                name: song.name,
                artistName: song.artistName,
                albumName: song.albumName,
                albumReleaseYear: song.albumReleaseYear
            })).join('');
        }

        form.innerHTML = `
            <div><label for="playlistName">Playlist Name:</label><input type="text" id="playlistName" name="name" required></div>
            <fieldset>
                <legend>Select Songs to Add:</legend>
                ${songsHtml}
            </fieldset>
            <button type="submit">Create Playlist</button>
            <div id="createPlaylistMessage" class="message-area"></div>
        `;
        form.addEventListener('submit', handleCreatePlaylist);
        sectionElement.appendChild(form);
    };
    
    /**
     * Handles song upload form submission
     * Called when upload song form is submitted
     * Validates file data and sends multipart form data to server
     */
    const handleUploadSong = (event) => {
        event.preventDefault();
        const form = event.target;
        const formData = new FormData(form);
        const messageArea = document.getElementById('uploadSongMessage');
        messageArea.textContent = 'Uploading...';

        makeCall('POST', '/api/songs', formData, (req) => {
            if (req.readyState === XMLHttpRequest.DONE) {
                try {
                    const response = JSON.parse(req.responseText);
                    if (req.status === 201 && response.status === 'success') {
                        messageArea.textContent = 'Song uploaded successfully!';
                        messageArea.className = 'message-area success';
                        State.updateSong(response.data); // Add to global songs list
                        form.reset();
                    } else {
                        messageArea.textContent = `Error: ${response.message || 'Upload failed.'} ${response.errors ? JSON.stringify(response.errors) : ''}`;
                        messageArea.className = 'message-area error';
                    }
                } catch (e) {
                     messageArea.textContent = 'Error processing server response or invalid JSON.';
                     messageArea.className = 'message-area error';
                }
            }
        }, false); // false for not resetting form by makeCall, we do it manually on success
    };

    /**
     * Handles playlist creation form submission
     * Called when create playlist form is submitted
     * Validates playlist name and selected songs, then sends data to server
     */
    const handleCreatePlaylist = (event) => {
        event.preventDefault();
        const form = event.target;
        const playlistName = form.name.value;
        const selectedSongElements = form.querySelectorAll('input[name="selectedSongs"]:checked');
        const songIDs = Array.from(selectedSongElements).map(el => parseInt(el.value));
        const messageArea = document.getElementById('createPlaylistMessage');
        messageArea.textContent = 'Creating...';

        if (!playlistName.trim()) {
            messageArea.textContent = 'Playlist name is required.';
            messageArea.className = 'message-area error';
            return;
        }
        if (songIDs.length === 0) {
            messageArea.textContent = 'Select at least one song.';
            messageArea.className = 'message-area error';
            return;
        }

        const payload = { name: playlistName, songIDs: songIDs };

        makeCall('POST', '/api/playlists', JSON.stringify(payload), (req) => {
            if (req.readyState === XMLHttpRequest.DONE) {
                 try {
                    const response = JSON.parse(req.responseText);
                    if (req.status === 201 && response.status === 'success') {
                        messageArea.textContent = 'Playlist created successfully!';
                        messageArea.className = 'message-area success';
                        console.log(response.data);
						State.setPlaylists(response.data);
						//State.updatePlaylist(response.data); // Add to global playlists
                        form.reset();
                    } else {
                        messageArea.textContent = `Error: ${response.message || 'Creation failed.'} ${response.errors ? JSON.stringify(response.errors) : ''}`;
                        messageArea.className = 'message-area error';
                    }
                } catch (e) {
                     messageArea.textContent = 'Error processing server response or invalid JSON.';
                     messageArea.className = 'message-area error';
                }
            }
        }, false);
    };
    
    /**
     * Handles playlist deletion with user confirmation
     * Called when delete button is clicked on a playlist item
     * Shows confirmation dialog and sends DELETE request to server
     */
    const handleDeletePlaylist = (playlistId) => {
        if (!confirm("Are you sure you want to delete this playlist? This action cannot be undone.")) {
            return;
        }
        const messageArea = document.getElementById('home-playlists-section').querySelector('p') || document.createElement('p'); // find or create a place for messages
        if (!messageArea.parentElement) document.getElementById('home-playlists-section').prepend(messageArea);
        messageArea.textContent = 'Deleting...';
        messageArea.className = 'message-area';


        makeCall('DELETE', `/api/playlists/${playlistId}`, null, (req) => {
            if (req.readyState === XMLHttpRequest.DONE) {
                if (req.status === 204) {
                    messageArea.textContent = 'Playlist deleted successfully.';
                    messageArea.className = 'message-area success';
                    State.removePlaylistById(playlistId); // Update state
                } else {
                    try {
                        const response = JSON.parse(req.responseText);
                        messageArea.textContent = `Error: ${response.message || 'Deletion failed.'}`;
                        messageArea.className = 'message-area error';
                    } catch (e) {
                        messageArea.textContent = `Error: Deletion failed (status ${req.status}).`;
                        messageArea.className = 'message-area error';
                    }
                }
                 setTimeout(() => { if(messageArea.textContent.includes("successfully")) messageArea.textContent = '';}, 3000);
            }
        });
    };


    return {
        render
    };
})();

