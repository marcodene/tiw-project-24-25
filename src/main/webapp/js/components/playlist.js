const PlaylistComponent = (() => {
    let container; // Main container for this component's view
    let currentPlaylistObj; // Stores the playlist object passed by the router
    let currentPage = 1;
    const songsPerPage = 5;
	
	/**
	 * Validates if a page number is within valid range
	 * Called by pagination controls to ensure page navigation is valid
	 * Returns true if page is integer between 1 and totalPages
	 */
	const validatePageNumber = (page, totalPages) => {
	        return Number.isInteger(page) && page >= 1 && page <= Math.max(1, totalPages);
	    };

	/**
	 * Renders the complete playlist detail view with songs and forms
	 * Called by Router when navigating to 'playlist' route
	 * Fetches full playlist data from server and displays songs with pagination
	 */
	const renderDetails = (appContainer, playlist) => {
	    container = appContainer;
	    container.innerHTML = ''; // Clear previous content
	    currentPage = 1; // Reset to first page
	    
	    // Show loading while fetching details
	    container.innerHTML = '<p>Loading playlist...</p>';
	    
	    // GET REQUEST FOR COMPLETE PLAYLIST DETAILS
	    makeCall('GET', `/api/playlists/${playlist.ID}`, null, (req) => {
	        if (req.readyState === XMLHttpRequest.DONE) {
	            if (req.status === 200) {
	                const response = JSON.parse(req.responseText);
	                if (response.status === 'success') {
	                    // Now we have the complete playlist with all songs
	                    currentPlaylistObj = response.data;
	                    State.setCurrentPlaylist(currentPlaylistObj);
	                    
	                    // Render the complete view
	                    container.innerHTML = '';
	                    
	                    const header = document.createElement('h2');
	                    header.textContent = `Playlist: ${currentPlaylistObj.name}`;
	                    container.appendChild(header);

	                    const actionButtons = document.createElement('div');
	                    actionButtons.className = 'playlist-actions';
	                    
	                    const backButton = document.createElement('button');
	                    backButton.textContent = 'Back to Home';
	                    backButton.className = 'back-link';
	                    backButton.addEventListener('click', () => Router.navigateTo('home'));
	                    
	                    const reorderButton = document.createElement('button');
	                    reorderButton.textContent = 'Reorder Songs';
	                    reorderButton.className = 'reorder-btn';
	                    reorderButton.addEventListener('click', () => {
	                        if (typeof ReorderComponent !== 'undefined' && ReorderComponent.showModal) {
	                            ReorderComponent.showModal(currentPlaylistObj);
	                        } else {
	                            alert('Reorder functionality not available yet.');
	                        }
	                    });
	                    
	                    actionButtons.appendChild(backButton);
	                    actionButtons.appendChild(reorderButton);
	                    container.appendChild(actionButtons);

	                    const songsDisplaySection = document.createElement('section');
	                    songsDisplaySection.id = 'playlist-songs-display';
	                    container.appendChild(songsDisplaySection);

	                    const addSongsSection = document.createElement('section');
	                    addSongsSection.id = 'playlist-add-songs';
	                    container.appendChild(addSongsSection);

	                    renderSongGrid(songsDisplaySection);
	                    renderAddSongsForm(addSongsSection);
	                }
	            } else {
	                container.innerHTML = '<p>Error loading playlist</p>';
	            }
	        }
	    });
	};

    /**
     * Renders paginated grid of songs in the current playlist
     * Called by renderDetails() and when pagination controls are used
     * Displays song cards with cover images and handles click navigation to player
     */
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
            const coverPath = `${baseURL}/GetFile${song.albumCoverPath}`;
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

	/**
	 * Renders pagination controls for navigating through songs
	 * Called by renderSongGrid() when there are multiple pages of songs
	 * Creates previous/next buttons and page information display
	 */
	const renderPaginationControls = (sectionElement, totalPages) => {
	    const existingControls = sectionElement.querySelector('.pagination-controls');
	    if(existingControls) existingControls.remove();

	    // Do not show control buttons if only one page
	    if (totalPages <= 1) return; // early exit

	    const controls = document.createElement('div');
	    controls.className = 'pagination-controls';

	    // Created only if it's not first page
	    if (currentPage > 1) {
	        const prevButton = document.createElement('button');
	        prevButton.textContent = 'Previous';
	        prevButton.addEventListener('click', () => {
	            if (currentPage > 1 && validatePageNumber(currentPage - 1, totalPages)) {
	                currentPage--;
	                renderSongGrid(sectionElement.querySelector('#playlist-songs-display') || sectionElement);
	            }
	        });
	        controls.appendChild(prevButton);
	    }

	    // Page info (always present)
	    const pageInfo = document.createElement('span');
	    pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;
	    controls.appendChild(pageInfo);

	    //  Created only if it's not last page
	    if (currentPage < totalPages) {
	        const nextButton = document.createElement('button');
	        nextButton.textContent = 'Next';
	        nextButton.addEventListener('click', () => {
	            if (currentPage < totalPages && validatePageNumber(currentPage + 1, totalPages)) {
	                currentPage++;
	                renderSongGrid(sectionElement.querySelector('#playlist-songs-display') || sectionElement);
	            }
	        });
	        controls.appendChild(nextButton); 
	    }

	    sectionElement.appendChild(controls);
	};
    
    /**
     * Renders form for adding existing songs to the current playlist
     * Called by renderDetails() and when songs state changes
     * Shows checkboxes for user's songs not already in playlist
     */
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

    /**
     * Handles adding selected songs to the current playlist
     * Called when add songs form is submitted
     * Validates selection and sends PATCH request to update playlist
     */
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
	
	// Subscription for when current playlist is modified
    State.subscribe('currentPlaylistChanged', (updatedPlaylist) => {
        if (updatedPlaylist && currentPlaylistObj && updatedPlaylist.ID === currentPlaylistObj.ID) {
            currentPlaylistObj = updatedPlaylist;
            const songsDisplay = document.getElementById('playlist-songs-display');
            const addSongs = document.getElementById('playlist-add-songs');
            if (songsDisplay) renderSongGrid(songsDisplay);
            if (addSongs) renderAddSongsForm(addSongs);
        }
    });

    // Subscription for when global songs list changes
    State.subscribe('songsChanged', () => {
        const addSongs = document.getElementById('playlist-add-songs');
        if (addSongs && currentPlaylistObj) {
            renderAddSongsForm(addSongs);
        }
    });
    
    return {
        renderDetails
    };
})();
