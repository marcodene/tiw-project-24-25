const PlaylistComponent = (() => {
    let container; // Main container for this component's view
    let currentPlaylistObj; // Stores the playlist object passed by the router
    let currentPage = 1;
    const songsPerPage = 5;

    const renderDetails = (appContainer, playlist) => {
        container = appContainer;
        container.innerHTML = ''; // Clear previous content
        currentPlaylistObj = playlist;
        currentPage = 1; // Reset to first page whenever a new playlist is rendered

        if (!currentPlaylistObj) {
            container.innerHTML = '<p>No playlist selected or playlist data is missing.</p>';
            // Optionally, navigate back to home or show a more specific error
            // Router.navigateTo('home'); 
            return;
        }
        
        const header = document.createElement('h2');
        header.textContent = `Playlist: ${currentPlaylistObj.name}`;
        container.appendChild(header);

        const backButton = document.createElement('button');
        backButton.textContent = 'Back to Home';
        backButton.addEventListener('click', () => Router.navigateTo('home'));
        container.appendChild(backButton);

        const songsDisplaySection = document.createElement('section');
        songsDisplaySection.id = 'playlist-songs-display';
        container.appendChild(songsDisplaySection);

        const addSongsSection = document.createElement('section');
        addSongsSection.id = 'playlist-add-songs';
        container.appendChild(addSongsSection);

        renderSongGrid(songsDisplaySection);
        renderAddSongsForm(addSongsSection);

        // Subscribe to changes in the current playlist to re-render song grid
        // This handles cases like songs being added, or order changing.
        State.subscribe('currentPlaylistChanged', (updatedPlaylist) => {
            if (updatedPlaylist && currentPlaylistObj && updatedPlaylist.ID === currentPlaylistObj.ID) {
                currentPlaylistObj = updatedPlaylist; // Update local copy
                // currentPage = 1; // Reset to first page on any modification
                renderSongGrid(songsDisplaySection); // Re-render songs
                renderAddSongsForm(addSongsSection); // Re-render add songs form (available songs might change)
            }
        });
         State.subscribe('songsChanged', () => { // If global songs list changes
            renderAddSongsForm(addSongsSection);
        });
    };

    const renderSongGrid = (sectionElement) => {
        sectionElement.innerHTML = '<h3>Songs in this Playlist</h3>';

        if (!currentPlaylistObj.songs || currentPlaylistObj.songs.length === 0) {
            sectionElement.innerHTML += '<p>This playlist is empty.</p>';
            return;
        }
        
        // Apply custom order if present (songs in currentPlaylistObj.songs should already be ordered by DAO)
        // The DAO now returns songs in the correct order (custom or default).

        const songsToDisplay = currentPlaylistObj.songs;
        const totalSongs = songsToDisplay.length;
        const totalPages = Math.ceil(totalSongs / songsPerPage);

        const startIndex = (currentPage - 1) * songsPerPage;
        const endIndex = startIndex + songsPerPage;
        const paginatedSongs = songsToDisplay.slice(startIndex, endIndex);

        const grid = document.createElement('div');
        grid.className = 'song-grid';

        paginatedSongs.forEach(song => {
            const songCard = document.createElement('div');
            songCard.className = 'song-card';
            // Use a placeholder if albumCoverPath is null or empty
            const coverPath = song.albumCoverPath ? `/uploads/${song.albumCoverPath}` : '/path/to/default/cover.png'; // Adjust default path
            songCard.innerHTML = `
                <img src="${coverPath}" alt="${song.albumName}" width="100" height="100" style="object-fit: cover;">
                <h4>${song.name}</h4>
                <p>${song.artistName}</p>
            `;
            songCard.addEventListener('click', () => {
                State.setCurrentSong(song);
                Router.navigateTo('player', song);
            });
            grid.appendChild(songCard);
        });
        sectionElement.appendChild(grid);

        renderPaginationControls(sectionElement, totalPages);
    };

    const renderPaginationControls = (sectionElement, totalPages) => {
        const existingControls = sectionElement.querySelector('.pagination-controls');
        if(existingControls) existingControls.remove();

        if (totalPages <= 1) return;

        const controls = document.createElement('div');
        controls.className = 'pagination-controls';

        const prevButton = document.createElement('button');
        prevButton.textContent = 'Previous';
        prevButton.disabled = currentPage === 1;
        prevButton.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage--;
                renderSongGrid(sectionElement.querySelector('#playlist-songs-display') || sectionElement);
            }
        });

        const nextButton = document.createElement('button');
        nextButton.textContent = 'Next';
        nextButton.disabled = currentPage === totalPages;
        nextButton.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                renderSongGrid(sectionElement.querySelector('#playlist-songs-display') || sectionElement);
            }
        });
        
        const pageInfo = document.createElement('span');
        pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;

        controls.appendChild(prevButton);
        controls.appendChild(pageInfo);
        controls.appendChild(nextButton);
        sectionElement.appendChild(controls);
    };
    
    const renderAddSongsForm = (sectionElement) => {
        sectionElement.innerHTML = '<h3>Add More Songs</h3>';
        const allUserSongs = State.getSongs(); // All songs uploaded by the user
        const songsInPlaylistIds = new Set(currentPlaylistObj.songs.map(s => s.ID));

        const availableSongs = allUserSongs.filter(song => !songsInPlaylistIds.has(song.ID));

        if (availableSongs.length === 0) {
            sectionElement.innerHTML += '<p>No other songs available to add from your library.</p>';
            return;
        }

        const form = document.createElement('form');
        form.id = 'addSongsToPlaylistForm';
        
        let songsHtml = availableSongs.map(song => `
            <div>
                <input type="checkbox" id="addsong-${song.ID}" name="selectedSongsToAdd" value="${song.ID}">
                <label for="addsong-${song.ID}">${song.name} - ${song.artistName}</label>
            </div>
        `).join('');

        form.innerHTML = `
            <fieldset>
                <legend>Select Songs from Your Library:</legend>
                ${songsHtml}
            </fieldset>
            <button type="submit">Add Selected Songs</button>
            <div id="addSongsMessage" class="message-area"></div>
        `;
        form.addEventListener('submit', handleAddSongsToPlaylist);
        sectionElement.appendChild(form);
    };

    const handleAddSongsToPlaylist = (event) => {
        event.preventDefault();
        const form = event.target;
        const selectedSongElements = form.querySelectorAll('input[name="selectedSongsToAdd"]:checked');
        const songIDsToAdd = Array.from(selectedSongElements).map(el => parseInt(el.value));
        const messageArea = document.getElementById('addSongsMessage');
        messageArea.textContent = 'Adding songs...';

        if (songIDsToAdd.length === 0) {
            messageArea.textContent = 'Please select at least one song to add.';
            messageArea.className = 'message-area error';
            return;
        }

        const payload = { songIDs: songIDsToAdd };
        // Use the overridden makeCall for JSON if it's still in Home.js, or use a proper JSON utility
        // Assuming makeCall from utils.js is now capable or we have a makeJsonCall
        makeCall('POST', `/api/playlists/${currentPlaylistObj.ID}/songs`, JSON.stringify(payload), (req) => {
            if (req.readyState === XMLHttpRequest.DONE) {
                try {
                    const response = JSON.parse(req.responseText);
                    if (req.status === 200 && response.status === 'success') { // API returns 200 OK on update
                        messageArea.textContent = 'Songs added successfully!';
                        messageArea.className = 'message-area success';
                        State.setCurrentPlaylist(response.data); // Update current playlist in state
                        currentPage = 1; // Reset to first page as per requirement
                        renderSongGrid(document.getElementById('playlist-songs-display')); // Re-render songs
                        renderAddSongsForm(document.getElementById('playlist-add-songs')); // Re-render form
                        form.reset();
                    } else {
                        messageArea.textContent = `Error: ${response.message || 'Failed to add songs.'}`;
                        messageArea.className = 'message-area error';
                    }
                } catch (e) {
                    messageArea.textContent = 'Error processing server response or invalid JSON.';
                    messageArea.className = 'message-area error';
                }
            }
        }, false); // Assuming makeCall can handle JSON if data is string and Content-Type is set by it
                   // Need to ensure makeCall sets Content-Type for JSON string.
    };
    
    return {
        renderDetails
    };
})();
