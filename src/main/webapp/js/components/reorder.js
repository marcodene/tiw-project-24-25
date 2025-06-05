const ReorderComponent = (() => {
    let modalElement;
    let playlistToListElement;
    let currentPlaylistForReorder;
    let draggedItem = null;

	const createModal = () => {
	    if (document.getElementById('reorderPlaylistModal')) return;

	    modalElement = document.createElement('div');
	    modalElement.id = 'reorderPlaylistModal';
	    modalElement.className = 'modal'; // Solo classe CSS, nessuno stile inline
	    modalElement.style.display = 'none'; // Solo questa proprietà inline per nascondere/mostrare

	    const modalContent = document.createElement('div');
	    modalContent.className = 'modal-content'; // Solo classe CSS

	    const closeButton = document.createElement('span');
	    closeButton.className = 'close-button';
	    closeButton.innerHTML = '&times;'; // "x" character
	    closeButton.onclick = closeModal;

	    const title = document.createElement('h3');
	    title.id = 'reorderModalTitle';
	    title.textContent = 'Riordino Playlist';

	    playlistToListElement = document.createElement('ul');
	    playlistToListElement.id = 'reorderPlaylistSongList';
	    // Rimuovi tutti gli stili inline - saranno gestiti dal CSS

	    const saveButton = document.createElement('button');
	    saveButton.textContent = 'Salva Ordinamento';
	    saveButton.onclick = handleSaveOrder;

	    const cancelButton = document.createElement('button');
	    cancelButton.textContent = 'Annulla';
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

	    // Drag and Drop event listeners per il container
	    playlistToListElement.addEventListener('dragover', handleDragOver);
	    playlistToListElement.addEventListener('drop', handleDrop);
	};

	const showModal = (playlist) => {
	    if (!modalElement) createModal();
	    currentPlaylistForReorder = playlist;
	    document.getElementById('reorderModalTitle').textContent = `Riordino: ${playlist.name}`;
	    
	    // Mostra messaggio di caricamento
	    const messageArea = document.getElementById('reorderMessageArea');
	    messageArea.textContent = 'Caricamento dettagli playlist...';
	    messageArea.className = 'message-area';

	    // Fetch detailed playlist information including songs
	    makeCall('GET', `/api/playlists/${playlist.ID}`, null, (req) => {
	        if (req.readyState === XMLHttpRequest.DONE) {
	            if (req.status === 200) {
	                try {
	                    const response = JSON.parse(req.responseText);
	                    if (response.status === 'success') {
	                        // Aggiorna currentPlaylistForReorder con i dati completi incluse le canzoni
	                        currentPlaylistForReorder = response.data;
	                        
	                        // Renderizza la lista delle canzoni ora che abbiamo i dati completi
	                        renderSongListForReorder([...response.data.songs]);
	                        
	                        // Pulisce il messaggio di caricamento
	                        messageArea.textContent = '';
	                        messageArea.className = 'message-area';
	                    } else {
	                        messageArea.textContent = `Errore: ${response.message || 'Impossibile caricare i dettagli della playlist.'}`;
	                        messageArea.className = 'message-area error';
	                    }
	                } catch (e) {
	                    messageArea.textContent = 'Errore nella elaborazione della risposta del server.';
	                    messageArea.className = 'message-area error';
	                }
	            } else {
	                messageArea.textContent = `Errore nel caricamento della playlist (Status: ${req.status}).`;
	                messageArea.className = 'message-area error';
	            }
	        }
	    });

	    // Mostra il modal immediatamente (con il messaggio di caricamento)
	    modalElement.style.display = 'block';
	};
	
    const closeModal = () => {
        if (modalElement) {
            modalElement.style.display = 'none';
        }
        playlistToListElement.innerHTML = ''; // Clear list
        currentPlaylistForReorder = null;
        draggedItem = null;
    };

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
    const handleDragStart = (e) => {
        draggedItem = e.target; // The <li> element being dragged
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', null); // Necessary for Firefox
        // Optionally, add a class to the dragged item for styling
        draggedItem.classList.add('dragging'); 
    }; 

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
