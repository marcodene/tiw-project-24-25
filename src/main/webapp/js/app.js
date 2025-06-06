const App = (() => {
    let currentUser = null; // Kept for quick access, but State.getCurrentUser() is canonical

    const init = (userData) => {
        State.setCurrentUser(userData);
        currentUser = userData; // Local cache for convenience
        
        console.log("App initialized for user:", currentUser.username);
        document.getElementById('app-container').style.display = 'block';
        
        Router.init(); // Initialize the router
        // Router.navigateTo('home'); // Navigate to home view after login

        // Setup main app layout and components (e.g., header, nav)
        const appNav = document.getElementById('app-nav');
        if (appNav) {
            // Example: Dynamic nav based on user, or just ensure logout is there
        }
        
        const logoutButton = document.getElementById('logoutButton');
        if (logoutButton && !logoutButton.hasAttribute('listener-attached')) {
            logoutButton.addEventListener('click', Auth.handleLogout); // Auth.handleLogout should call State.resetState()
            logoutButton.setAttribute('listener-attached', 'true');
        }
        
        // Initial data fetching for the app
        fetchInitialData();
        
        // Default navigation
        Router.navigateTo('home');
    };

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

// Modify Auth.js to call State.resetState() on logout and App.reset()
// This part is tricky as subtasks don't easily modify multiple files based on conditions.
// We'll assume Auth.handleLogout will be updated to include:
// State.resetState(); App.reset(); // before showing login form.
// For now, App.reset() calls State.resetState().
// And Auth.handleLogout in auth.js already calls App.reset().
