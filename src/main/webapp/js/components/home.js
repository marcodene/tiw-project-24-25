const Home = (() => {
    let container; // To store the main container element for the home view

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

        const createPlaylistSection = document.createElement('section');
        createPlaylistSection.id = 'home-create-playlist-section';
        container.appendChild(createPlaylistSection);
        
        const uploadSongSection = document.createElement('section');
        uploadSongSection.id = 'home-upload-song-section';
        container.appendChild(uploadSongSection);

        renderPlaylists(playlistsSection);
        renderCreatePlaylistForm(createPlaylistSection);
        renderUploadSongForm(uploadSongSection);

        // Subscribe to state changes to re-render if necessary
        State.subscribe('playlistsChanged', () => renderPlaylists(playlistsSection));
        State.subscribe('songsChanged', () => renderCreatePlaylistForm(createPlaylistSection));
        State.subscribe('genresChanged', () => renderUploadSongForm(uploadSongSection)); // Re-render if genres load after form
    };

    const renderPlaylists = (sectionElement) => {
        sectionElement.innerHTML = '<h3>Your Playlists</h3>';
        const playlists = State.getPlaylists();
        if (!playlists || playlists.length === 0) {
            sectionElement.innerHTML += '<p>You have no playlists yet. Create one below!</p>';
            return;
        }

        const ul = document.createElement('ul');
        ul.className = 'playlist-list';
        playlists.forEach(playlist => {
            const li = document.createElement('li');
            li.className = 'playlist-item';
            
            const nameSpan = document.createElement('span');
            nameSpan.textContent = playlist.name;
            nameSpan.style.cursor = 'pointer';
            nameSpan.addEventListener('click', () => {
                State.setCurrentPlaylist(playlist); // Set the full playlist object in state
                Router.navigateTo('playlist', playlist); // Pass the full playlist object
            });

            const reorderButton = document.createElement('button');
            reorderButton.textContent = 'Riordina';
            reorderButton.className = 'button-reorder';
            reorderButton.addEventListener('click', (e) => {
                e.stopPropagation(); // Prevent li click event
                // Ensure ReorderComponent is available and show modal
                if (typeof ReorderComponent !== 'undefined' && ReorderComponent.showModal) {
                    ReorderComponent.showModal(playlist);
                } else {
                    alert('Reorder functionality not available yet.');
                }
            });
            
            const deleteButton = document.createElement('button');
            deleteButton.textContent = 'Delete';
            deleteButton.className = 'button-delete';
            deleteButton.addEventListener('click', (e) => {
                e.stopPropagation();
                handleDeletePlaylist(playlist.ID);
            });

            li.appendChild(nameSpan);
            li.appendChild(reorderButton);
            li.appendChild(deleteButton);
            ul.appendChild(li);
        });
        sectionElement.appendChild(ul);
    };

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
            <div><label for="songImageFile">Cover Image (JPG, PNG):</label><input type="file" id="songImageFile" name="imageFile" accept=".jpg,.jpeg,.png"></div>
            <button type="submit">Upload Song</button>
            <div id="uploadSongMessage" class="message-area"></div>
        `;
        form.addEventListener('submit', handleUploadSong);
        sectionElement.appendChild(form);
    };

    const renderCreatePlaylistForm = (sectionElement) => {
        sectionElement.innerHTML = '<h3>Create New Playlist</h3>';
        const allSongs = State.getSongs(); // Assuming all user songs are loaded in State

        const form = document.createElement('form');
        form.id = 'createPlaylistForm';
        let songsHtml = '<p>No songs available to add. Upload songs first.</p>';
        if (allSongs && allSongs.length > 0) {
            songsHtml = allSongs.map(song => `
                <div>
                    <input type="checkbox" id="song-${song.ID}" name="selectedSongs" value="${song.ID}">
                    <label for="song-${song.ID}">${song.name} - ${song.artistName}</label>
                </div>
            `).join('');
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
                        State.updatePlaylist(response.data); // Add to global playlists
                        form.reset();
                        // Re-render create playlist form to clear checkboxes (or form.reset() handles it)
                        // If State.getSongs() is used by create playlist form, it's already up-to-date.
                    } else {
                        messageArea.textContent = `Error: ${response.message || 'Creation failed.'} ${response.errors ? JSON.stringify(response.errors) : ''}`;
                        messageArea.className = 'message-area error';
                    }
                } catch (e) {
                     messageArea.textContent = 'Error processing server response or invalid JSON.';
                     messageArea.className = 'message-area error';
                }
            }
        }, false); // Send JSON, so formElement is null and reset is false.
                  // Overload makeCall or use a different function for JSON payloads
                  // For now, adapting makeCall to send stringified payload (needs header set)
    };
    
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

// Ensure ReorderComponent is defined (placeholder for now if not created)
// const ReorderComponent = ReorderComponent || { showModal: (playlist) => alert(\`Reorder for \${playlist.name} clicked\`) };
