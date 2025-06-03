const PlayerComponent = (() => {
    let container; // Main container for this component's view
    let currentSongObj; // Stores the song object passed by the router

    const render = (appContainer, song) => {
        container = appContainer;
        container.innerHTML = ''; // Clear previous content
        currentSongObj = song;

        if (!currentSongObj) {
            container.innerHTML = '<p>No song selected or song data is missing.</p>';
            // Optionally, navigate back or show error
            Router.navigateTo('home'); // Or previous view if known
            return;
        }

        const header = document.createElement('h2');
        header.textContent = `Now Playing: ${currentSongObj.name}`;
        container.appendChild(header);

        // Button to go back to the previous view (e.g., playlist or home)
        const backButton = document.createElement('button');
        backButton.textContent = 'Go Back'; // Or be more specific, e.g., 'Back to Playlist'
        backButton.addEventListener('click', () => {
            // Simple back navigation: if there was a current playlist, go there, else home.
            const previousPlaylist = State.getCurrentPlaylist();
            if (previousPlaylist) {
                Router.navigateTo('playlist', previousPlaylist);
            } else {
                Router.navigateTo('home');
            }
        });
        container.appendChild(backButton);

        const detailsSection = document.createElement('section');
        detailsSection.id = 'player-details';
        container.appendChild(detailsSection);

        renderSongDetails(detailsSection);
        renderAudioPlayer(detailsSection); // Append player to the same section or a new one

        const actionsSection = document.createElement('section');
        actionsSection.id = 'player-actions';
        container.appendChild(actionsSection);
        renderPlayerActions(actionsSection);
    };

    const renderSongDetails = (sectionElement) => {
        const detailsDiv = document.createElement('div');
        detailsDiv.className = 'song-full-details';

        const coverPath = currentSongObj.albumCoverPath ? `${baseURL}/GetFile${currentSongObj.albumCoverPath}` : '/covers/default.jpg'; // Adjust default path
        
        detailsDiv.innerHTML = `
            <img src="${coverPath}" alt="Album cover for ${currentSongObj.albumName}" class="player-album-cover" style="max-width: 300px; margin-bottom: 15px;">
            <p><strong>Title:</strong> ${currentSongObj.name}</p>
            <p><strong>Artist:</strong> ${currentSongObj.artistName}</p>
            <p><strong>Album:</strong> ${currentSongObj.albumName} (${currentSongObj.albumReleaseYear})</p>
            <p><strong>Genre:</strong> ${currentSongObj.genre}</p>
        `;
        sectionElement.appendChild(detailsDiv);
    };

    const renderAudioPlayer = (sectionElement) => {
        const audioPlayerDiv = document.createElement('div');
        audioPlayerDiv.className = 'audio-player-container';
        
        const audioEl = document.createElement('audio');
        audioEl.controls = true;
        // Assuming audioFilePath is relative to '/uploads/'
        // e.g., "songs/xxxxxxxx-xxxx.mp3"
        // The base path should be handled by the server or a reverse proxy correctly.
        // If audioFilePath is stored as "songs/file.mp3", then src should be "/uploads/songs/file.mp3"
        audioEl.src = currentSongObj.audioFilePath ? `${baseURL}/GetFile${currentSongObj.audioFilePath}`  : '';
        audioEl.id = 'html5-audio-player';
        
        if (!audioEl.src) {
            audioPlayerDiv.innerHTML += '<p>Audio file not available for this song.</p>';
        } else {
            audioPlayerDiv.appendChild(audioEl);
        }
        sectionElement.appendChild(audioPlayerDiv);
    };

    const renderPlayerActions = (sectionElement) => {
        const deleteButton = document.createElement('button');
        deleteButton.textContent = 'Delete This Song';
        deleteButton.className = 'button-delete';
        deleteButton.addEventListener('click', handleDeleteSong);
        
        sectionElement.appendChild(deleteButton);
        
        const messageArea = document.createElement('div');
        messageArea.id = 'playerActionMessage';
        messageArea.className = 'message-area';
        sectionElement.appendChild(messageArea);
    };

    const handleDeleteSong = () => {
        if (!currentSongObj || !currentSongObj.ID) return;

        if (!confirm(`Are you sure you want to delete the song "${currentSongObj.name}"? This action cannot be undone.`)) {
            return;
        }

        const messageArea = document.getElementById('playerActionMessage');
        messageArea.textContent = 'Deleting song...';
        messageArea.className = 'message-area';

        makeCall('DELETE', `/api/songs/${currentSongObj.ID}`, null, (req) => {
            if (req.readyState === XMLHttpRequest.DONE) {
                if (req.status === 204) { // Successfully deleted
                    messageArea.textContent = 'Song deleted successfully.';
                    messageArea.className = 'message-area success';
                    
                    const deletedSongId = currentSongObj.ID;
                    State.removeSongById(deletedSongId); // Update global state

                    // Navigate away after deletion
                    setTimeout(() => {
                        const previousPlaylist = State.getCurrentPlaylist();
                        if (previousPlaylist) { // If player was opened from a playlist
                            // Check if the deleted song was part of that playlist
                            const songExistsInPrevPlaylist = previousPlaylist.songs.some(s => s.ID === deletedSongId);
                            if(songExistsInPrevPlaylist){
                                // Playlist in state already updated by State.removeSongById if it was currentPlaylist
                                Router.navigateTo('playlist', State.getCurrentPlaylist());
                            } else {
                                Router.navigateTo('home');
                            }
                        } else {
                            Router.navigateTo('home');
                        }
                    }, 1500); // Give time for user to see message

                } else {
                     try {
                        const response = JSON.parse(req.responseText);
                        messageArea.textContent = `Error: ${response.message || 'Deletion failed.'}`;
                    } catch (e) {
                        messageArea.textContent = `Error: Deletion failed (status ${req.status}).`;
                    }
                    messageArea.className = 'message-area error';
                }
            }
        });
    };

    return {
        render
    };
})();
