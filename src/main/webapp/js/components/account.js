const AccountManager = (() => {
    let container; // To store the main container element for the account view

    /**
     * Renders the account management interface
     * Called by Router when navigating to 'account' route
     * Creates user information display, danger zone section, and navigation buttons
     */
    const render = (appContainer) => {
        container = appContainer;
        container.innerHTML = ''; // Clear previous content

        const currentUser = State.getCurrentUser();
        if (!currentUser) {
            container.innerHTML = '<p>User not logged in</p>';
            return;
        }

        const accountSection = document.createElement('section');
        accountSection.className = 'account-management card';
        
        accountSection.innerHTML = `
            <h2>Account Management</h2>
            <div class="user-info">
                <h3>User Information</h3>
                <p><strong>Username:</strong> ${currentUser.username}</p>
                <p><strong>Name:</strong> ${currentUser.name}</p>
                <p><strong>Surname:</strong> ${currentUser.surname}</p>
            </div>
            
            <div class="danger-zone">
                <h3>Danger Zone</h3>
                <p class="warning-text">Once you delete your account, there is no going back. This will permanently delete all your songs, playlists, and account data.</p>
                <button id="deleteAccountBtn" class="delete-account-btn">Delete Account</button>
            </div>
            
            <div class="back-navigation">
                <button id="backToHomeBtn" class="back-btn">Back to Home</button>
            </div>
        `;

        container.appendChild(accountSection);

        setupEventListeners();
    };

    /**
     * Sets up event listeners for account management buttons
     * Called by render() to attach click handlers to interactive elements
     * Handles delete account button and back navigation
     */
    const setupEventListeners = () => {
        const deleteAccountBtn = document.getElementById('deleteAccountBtn');
        const backToHomeBtn = document.getElementById('backToHomeBtn');

        deleteAccountBtn.addEventListener('click', handleDeleteAccount);
        backToHomeBtn.addEventListener('click', () => {
            Router.navigateTo('home');
        });
    };

    /**
     * Handles the initial delete account confirmation
     * Called when delete account button is clicked
     * Shows confirmation dialog and initiates the deletion process if confirmed
     */
    const handleDeleteAccount = () => {
        const confirmDelete = confirm(
            "Are you absolutely sure you want to delete your account?\n\n" +
            "This action will:\n" +
            "• Permanently delete your account\n" +
            "• Delete all your songs and uploaded files\n" +
            "• Delete all your playlists\n" +
            "• Cannot be undone\n\n"
        );

        if (!confirmDelete) {
            return;
        }

        showPasswordConfirmationModal();
    };

    /**
     * Displays the password confirmation modal for account deletion
     * Called by handleDeleteAccount() after initial confirmation
     * Creates modal with password input form and handles user interactions
     */
    const showPasswordConfirmationModal = () => {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `
            <div class="modal-content">
                <h3>Confirm Account Deletion</h3>
                <p>Please enter your password to confirm account deletion:</p>
                <form id="deleteAccountForm">
                    <div class="form-group">
                        <label for="confirmPassword">Password:</label>
                        <input type="password" id="confirmPassword" name="password" required>
                    </div>
                    <div class="form-actions">
                        <button type="button" id="cancelDelete" class="cancel-btn">Cancel</button>
                        <button type="submit" class="delete-btn">Delete Account</button>
                    </div>
                    <div id="deleteMessage" class="message-area"></div>
                </form>
            </div>
        `;

        document.body.appendChild(modal);

        const form = document.getElementById('deleteAccountForm');
        const cancelBtn = document.getElementById('cancelDelete');
        const messageArea = document.getElementById('deleteMessage');

        form.addEventListener('submit', (e) => {
            e.preventDefault();
            const password = document.getElementById('confirmPassword').value;
            
            if (!password) {
                messageArea.textContent = 'Password is required';
                messageArea.className = 'message-area error';
                return;
            }

            performAccountDeletion(form, messageArea);
        });

        cancelBtn.addEventListener('click', () => {
            document.body.removeChild(modal);
        });

        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                document.body.removeChild(modal);
            }
        });
    };

    /**
     * Performs the actual account deletion request to the server
     * Called when password confirmation form is submitted
     * Sends form data to delete user API and handles the response
     */
    const performAccountDeletion = (form, messageArea) => {
        messageArea.textContent = 'Deleting account...';
        messageArea.className = 'message-area';

        makeCall('POST', '/api/deleteUser', form, (req) => {
            if (req.readyState === XMLHttpRequest.DONE) {
                try {
                    const response = JSON.parse(req.responseText);
                    
                    if (req.status === 200 && response.status === 'success') {
                        messageArea.textContent = 'Account deleted successfully. Redirecting...';
                        messageArea.className = 'message-area success';
                        
                        // Reset application state and redirect to login after successful deletion
                        setTimeout(() => {
                            App.reset();
                            Auth.showLoginForm();
                            window.location.replace('./');
                        }, 2000);
                        
                    } else {
                        messageArea.textContent = response.message || 'Account deletion failed';
                        messageArea.className = 'message-area error';
                    }
                } catch (e) {
                    messageArea.textContent = 'Error processing server response';
                    messageArea.className = 'message-area error';
                }
            }
        }, false);
    };

    return {
        render
    };
})();