const ReorderComponent = (() => {
    let modalElement;
    let playlistToListElement;
    let currentPlaylistForReorder;
    let draggedItem = null;

	/**
	 * Creates the modal HTML structure for playlist reordering
	 * Called by showModal() when modal doesn't exist yet
	 * Sets up modal content, drag and drop listeners, and buttons
	 */
	const createModal = () => {
	    if (document.getElementById('reorderPlaylistModal')) return;

	    modalElement = document.createElement('div');
	    modalElement.id = 'reorderPlaylistModal';
	    modalElement.className = 'modal'; // CSS class only, no inline styles
	    modalElement.style.display = 'none'; // Only this inline property to hide/show

	    const modalContent = document.createElement('div');
	    modalContent.className = 'modal-content'; // CSS class only

	    const closeButton = document.createElement('span');
	    closeButton.className = 'close-button';
	    closeButton.innerHTML = '&times;'; // "x" character
	    closeButton.onclick = closeModal;

	    const title = document.createElement('h3');
	    title.id = 'reorderModalTitle';
	    title.textContent = 'Reorder Playlist';

	    playlistToListElement = document.createElement('ul');
	    playlistToListElement.id = 'reorderPlaylistSongList';
	    // Remove all inline styles - will be handled by CSS

	    const saveButton = document.createElement('button');
	    saveButton.textContent = 'Save Order';
	    saveButton.onclick = handleSaveOrder;

	    const cancelButton = document.createElement('button');
	    cancelButton.textContent = 'Cancel';
	    cancelButton.onclick = closeModal;
	    
	    const messageArea = document.createElement('div');
	    messageArea.id = 'reorderMessageArea';
	    messageArea.className = 'message-area';

	    modalContent.appendChild(closeButton);
	    modalContent.appendChild(title);
	    modalContent.appendChild(playlistToListElement);
	    modalContent.appendChild(saveButton);
	    modalContent.appendChild(cancelButton);
	    modalContent.appendChild(messageArea);
	    modalElement.appendChild(modalContent);
	    document.body.appendChild(modalElement);

	    // Drag and Drop event listeners for the container
	    playlistToListElement.addEventListener('dragover', handleDragOver);
	    playlistToListElement.addEventListener('drop', handleDrop);
	};

	/**
	 * Shows the reorder modal for a specific playlist
	 * Called by Home component when reorder button is clicked
	 * Fetches playlist details and displays draggable song list
	 */
	const showModal = (playlist) => {
	    if (!modalElement) createModal();
	    currentPlaylistForReorder = playlist;
	    document.getElementById('reorderModalTitle').textContent = `Reorder: ${playlist.name}`;
	    
	    // Show loading message
	    const messageArea = document.getElementById('reorderMessageArea');
	    messageArea.textContent = 'Loading playlist details...';
	    messageArea.className = 'message-area';

	    // Fetch detailed playlist information including songs
	    makeCall('GET', `/api/playlists/${playlist.ID}`, null, (req) => {
	        if (req.readyState === XMLHttpRequest.DONE) {
	            if (req.status === 200) {
	                try {
	                    const response = JSON.parse(req.responseText);
	                    if (response.status === 'success') {
							// Update currentPlaylistForReorder with complete data including songs
	                        currentPlaylistForReorder = response.data;
	                        
							// Render songs list with complete data
	                        renderSongListForReorder([...response.data.songs]);
	                        
	                        // Clean loading message
	                        messageArea.textContent = '';
	                        messageArea.className = 'message-area';
	                    } else {
	                        messageArea.textContent = `Error: ${response.message || 'Unable to load playlist details.'}`;
	                        messageArea.className = 'message-area error';
	                    }
	                } catch (e) {
	                    messageArea.textContent = 'Error processing server response.';
	                    messageArea.className = 'message-area error';
	                }
	            } else {
	                messageArea.textContent = `Error loading playlist (Status: ${req.status}).`;
	                messageArea.className = 'message-area error';
	            }
	        }
	    });

		// Show immediately the modal (with loading message)
	    modalElement.style.display = 'block';
	};
	
    /**
     * Closes the reorder modal and resets state
     * Called when close button or cancel button is clicked
     * Clears modal content and resets drag and drop state
     */
    const closeModal = () => {
        if (modalElement) {
            modalElement.style.display = 'none';
        }
        playlistToListElement.innerHTML = ''; // Clear list
        currentPlaylistForReorder = null;
        draggedItem = null;
    };

	/**
	 * Renders the list of songs as draggable items for reordering
	 * Called by showModal() after fetching playlist details
	 * Creates draggable list items with song names and artist information
	 */
	const renderSongListForReorder = (songs) => {
	    playlistToListElement.innerHTML = ''; // Clear previous items
	    songs.forEach((song, index) => {
	        const li = document.createElement('li');
	        li.textContent = `${song.name} - ${song.artistName}`;
	        li.setAttribute('data-song-id', song.ID);
	        li.setAttribute('draggable', true);

	        // Drag and Drop event listeners for each item
	        li.addEventListener('dragstart', handleDragStart);
	        
	        playlistToListElement.appendChild(li);
	    });
	};

    // --- Drag and Drop Handlers ---
    /**
     * Handles start of drag operation
     * Called when user starts dragging a song item
     * Sets up drag data and visual feedback
     */
    const handleDragStart = (e) => {
        draggedItem = e.target; // The <li> element being dragged
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', null); // Necessary for Firefox
        // Optionally, add a class to the dragged item for styling
        draggedItem.classList.add('dragging'); 
    }; 

    /**
     * Handles drag over events for reordering logic
     * Called continuously while dragging over the list
     * Determines drop position and rearranges items dynamically
     */
    const handleDragOver = (e) => {
        e.preventDefault(); // Necessary to allow dropping
        e.dataTransfer.dropEffect = 'move';
        const target = e.target.closest('li'); // Find the <li> element being hovered over
        if (target && target !== draggedItem && target.parentNode === playlistToListElement) {
            // Determine if dragging above or below the target item
            const rect = target.getBoundingClientRect();
            const offsetY = e.clientY - rect.top;
            const isAfter = offsetY > target.offsetHeight / 2;

            if (isAfter) {
                playlistToListElement.insertBefore(draggedItem, target.nextSibling);
            } else {
                playlistToListElement.insertBefore(draggedItem, target);
            }
        } else if (!target && draggedItem.parentNode === playlistToListElement) {
            // If dragging over empty space in the list (e.g., to the end)
            // This case is implicitly handled if the list itself is the drop target
            // and no specific <li> is under the cursor.
        }
    };

    /**
     * Handles drop event to finalize item reordering
     * Called when user releases dragged item
     * Cleans up drag state and finalizes position
     */
    const handleDrop = (e) => {
        e.preventDefault();
        if (draggedItem) {
            draggedItem.classList.remove('dragging');
            // The actual reordering of <li> elements is done in handleDragOver.
            // Here, we just finalize.
            draggedItem = null;
        }
    };
    // --- End Drag and Drop Handlers ---

    /**
     * Handles saving the new song order to the server
     * Called when save button is clicked in reorder modal
     * Collects current order from DOM and sends PUT request to update playlist
     */
    const handleSaveOrder = () => {
        const messageArea = document.getElementById('reorderMessageArea');
        messageArea.textContent = 'Saving order...';
        messageArea.className = 'message-area';

        const songListItems = playlistToListElement.querySelectorAll('li');
        const newSongOrderIds = Array.from(songListItems).map(li => parseInt(li.getAttribute('data-song-id')));

        if (!currentPlaylistForReorder || !currentPlaylistForReorder.ID) {
            messageArea.textContent = 'Error: No playlist context for saving.';
            messageArea.className = 'message-area error';
            return;
        }

        const payload = { songIDs: newSongOrderIds };

        makeCall('PUT', `/api/playlists/${currentPlaylistForReorder.ID}/order`, JSON.stringify(payload), (req) => {
            if (req.readyState === XMLHttpRequest.DONE) {
                 try {
                    const response = JSON.parse(req.responseText);
                    if (req.status === 200 && response.status === 'success') {
                        messageArea.textContent = 'Order saved successfully!';
                        messageArea.className = 'message-area success';
                        State.updatePlaylist(response.data); // Update playlist in global state
                        setTimeout(closeModal, 1000); // Close modal after a short delay
                    } else {
                        messageArea.textContent = `Error: ${response.message || 'Failed to save order.'}`;
                        messageArea.className = 'message-area error';
                    }
                } catch (e) {
                    messageArea.textContent = 'Error processing server response or invalid JSON.';
                    messageArea.className = 'message-area error';
                }
            }
        });
    };
    
    // Ensure makeCall is available (it should be global from utils.js now)
    // No local override needed if utils.js is correctly updated and loaded.

    // Initialize modal structure once when script loads (or on first call to showModal)
    // createModal(); // Calling it here ensures it's ready. Or call in an init() if preferred.

    return {
        showModal,
        createModal // Expose if external initialization is desired
    };
})();

// Self-initialize the modal structure when the script is loaded.
document.addEventListener('DOMContentLoaded', () => {
    ReorderComponent.createModal();
});
