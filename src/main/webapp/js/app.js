const App = (() => {
    let currentUser = null; // Kept for quick access, but State.getCurrentUser() is canonical

    /**
     * Initializes the main application after authentication
     * Called by Auth.checkSessionAndSetup() when a valid session is found
     * Sets up global state, router and loads initial data
     */
    const init = (userData) => {
        State.setCurrentUser(userData);
        currentUser = userData; // Local cache for convenience
        
        console.log("App initialized for user:", currentUser.username);
        const appContainer = document.getElementById('app-container');
        if (appContainer) {
            appContainer.style.display = 'block';
        }
        
        // Update user greeting in header
        const userGreeting = document.getElementById('user-greeting');
        if (userGreeting) {
            userGreeting.textContent = `Hello, ${currentUser.name || currentUser.username}`;
        }
        
        Router.init(); // Initialize the router

        // Setup logout button
        const logoutButton = document.getElementById('logoutButton');
        if (logoutButton && !logoutButton.hasAttribute('listener-attached')) {
            logoutButton.addEventListener('click', Auth.handleLogout);
            logoutButton.setAttribute('listener-attached', 'true');
        }
        
        // Setup account button
        const accountButton = document.getElementById('accountButton');
        if (accountButton && !accountButton.hasAttribute('listener-attached')) {
            accountButton.addEventListener('click', () => {
                Router.navigateTo('account');
            });
            accountButton.setAttribute('listener-attached', 'true');
        }
        
        // Initial data fetching for the app
        fetchInitialData();
        
        // Default navigation
        Router.navigateTo('home');
    };

    /**
     * Loads data required at application startup
     * Called by init() to populate state with genres, playlists and songs
     * Uses asynchronous API calls to initialize global state
     */
    const fetchInitialData = () => {
        // Fetch genres
        makeCall('GET', '/api/genres', null, (req) => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                const response = JSON.parse(req.responseText);
                if (response.status === 'success') {
                    State.setGenres(response.data);
                }
            } // Basic error handling for brevity
        });

        // Fetch user's playlists
        makeCall('GET', '/api/playlists', null, (req) => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                const response = JSON.parse(req.responseText);
                if (response.status === 'success') {
                    State.setPlaylists(response.data);
                }
            }
        });
        
        // Fetch all user's songs (optional, could be fetched on demand)
        // For now, let's assume we load them for easier client-side operations
        makeCall('GET', '/api/songs', null, (req) => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                const response = JSON.parse(req.responseText);
                if (response.status === 'success') {
                    State.setSongs(response.data); // This sets the global list of songs
                }
            }
        });
    };

    /**
     * Completely resets application state
     * Called by Auth.handleLogout() before redirecting to login page
     * Clears all user data and interface state
     */
    const reset = () => {
        currentUser = null;
        State.resetState(); // Use State's reset function
        const appContent = document.getElementById('app-content');
        if(appContent) appContent.innerHTML = '';
        console.log("App reset.");
        // Auth.showLoginForm() will be called by Auth.handleLogout
    };
    
    return {
        init,
        reset
    };
})();
