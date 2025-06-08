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
        modalElement.className = 'modal';
        modalElement.style.display = 'none';

        const modalContent = document.createElement('div');
        modalContent.className = 'modal-content';

        const closeButton = document.createElement('span');
        closeButton.className = 'close-button';
        closeButton.innerHTML = '&times;';
        closeButton.addEventListener('click', closeModal);

        const title = document.createElement('h3');
        title.id = 'reorderModalTitle';
        title.textContent = 'Reorder Playlist';

        playlistToListElement = document.createElement('ul');
        playlistToListElement.id = 'reorderPlaylistSongList';

        const saveButton = document.createElement('button');
        saveButton.textContent = 'Save Order';
        saveButton.addEventListener('click', handleSaveOrder);

        const cancelButton = document.createElement('button');
        cancelButton.textContent = 'Cancel';
        cancelButton.addEventListener('click', closeModal);
        
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
        
        // Set up container-level event listeners (only once, here)
        playlistToListElement.addEventListener('dragover', handleDragOver);
        playlistToListElement.addEventListener('drop', handleDrop);
        playlistToListElement.addEventListener('dragenter', handleDragEnter);
        
        songs.forEach((song, index) => {
            const li = document.createElement('li');
            li.textContent = `${song.name} - ${song.artistName}`;
            li.setAttribute('data-song-id', song.ID);
            li.setAttribute('draggable', true);

            // Add drag and drop event listeners
            li.addEventListener('dragstart', handleDragStart);
            li.addEventListener('dragover', handleDragOver);
            li.addEventListener('dragenter', handleDragEnter);
            
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
        draggedItem = e.target;
        
        // Firefox requires both effectAllowed and proper data
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/html', e.target.innerHTML);
        e.dataTransfer.setData('text/plain', e.target.getAttribute('data-song-id'));
        
        // Add visual feedback after dragstart completes to avoid rendering conflicts
        setTimeout(() => {
            if (draggedItem) {
                draggedItem.classList.add('dragging');
            }
        }, 0);
    };

    /**
     * Handles drag enter events
     * REQUIRED for Firefox compatibility - Firefox needs preventDefault() 
     * on both dragenter and dragover events for drops to work
     */
    const handleDragEnter = (e) => {
        e.preventDefault();
        return false;
    };

    /**
     * Handles drag over events for reordering logic
     * Called continuously while dragging over the list
     * Determines drop position and rearranges items dynamically
     */
    const handleDragOver = (e) => {
        if (e.preventDefault) {
            e.preventDefault(); // Allows us to drop
        }
      
        e.dataTransfer.dropEffect = 'move';
        
        if (!draggedItem) return false;

        // Find the closest li element (handles both li and its children)
        let target = e.target;
        while (target && target.tagName !== 'LI' && target !== playlistToListElement) {
            target = target.parentNode;
        }
        
        if (target && target.tagName === 'LI' && target !== draggedItem && target.parentNode === playlistToListElement) {
            // Determine if dragging above or below the target item
            const rect = target.getBoundingClientRect();
            const offsetY = e.clientY - rect.top;
            const isAfter = offsetY > target.offsetHeight / 2;

            if (isAfter && target.nextSibling !== draggedItem) {
                // Insert after target
                playlistToListElement.insertBefore(draggedItem, target.nextSibling);
            } else if (!isAfter && target.previousSibling !== draggedItem) {
                // Insert before target
                playlistToListElement.insertBefore(draggedItem, target);
            }
        }
        
        return false;
    };

    /**
     * Handles drop event to finalize item reordering
     * Called when user releases dragged item
     * Cleans up drag state and finalizes position
     */
    const handleDrop = (e) => {
        if (e.stopPropagation) {
            e.stopPropagation(); // Stops some browsers from redirecting
        }
        
        if (e.preventDefault) {
            e.preventDefault();
        }
        
        if (draggedItem) {
            draggedItem.classList.remove('dragging');
            draggedItem = null;
        }
        
        return false;
    };
    

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

    return {
        showModal,
        createModal
    };
})();

// Self-initialize the modal structure when the script is loaded.
document.addEventListener('DOMContentLoaded', () => {
    ReorderComponent.createModal();
});