const Router = (() => {
    const appContentContainerId = 'app-content'; // ID of the div where content is rendered
    let appContentContainer;

    const routes = {
        // Each function should render its view into appContentContainer
        // and can take data (e.g., an ID for detail views)
        'home': (data) => {
            // Example: Home.render(appContentContainer, data);
            // This will be implemented by components/home.js
            if (typeof Home !== 'undefined' && Home.render) {
                Home.render(appContentContainer, data);
            } else {
                appContentContainer.innerHTML = '<h2>Home View (Component not fully loaded)</h2>';
            }
        },
        'playlist': (data) => { // data could be playlistId or playlist object
            // Example: PlaylistDetail.render(appContentContainer, data);
            if (typeof PlaylistComponent !== 'undefined' && PlaylistComponent.renderDetails) {
                PlaylistComponent.renderDetails(appContentContainer, data);
            } else {
                appContentContainer.innerHTML = '<h2>Playlist Detail View (Component not fully loaded)</h2>';
            }
        },
        'player': (data) => { // data could be songId or song object
            // Example: Player.render(appContentContainer, data);
            if (typeof PlayerComponent !== 'undefined' && PlayerComponent.render) {
                PlayerComponent.render(appContentContainer, data);
            } else {
                appContentContainer.innerHTML = '<h2>Player View (Component not fully loaded)</h2>';
            }
        },
        // Add more routes as needed, e.g., 'uploadSongForm', 'createPlaylistForm'
        // These might be part of 'home' or separate components.
    };

    /**
     * Initializes the router by setting up DOM references and state subscriptions
     * Called by App.init() during application startup
     * Sets up the main content container and subscribes to view changes
     */
    const init = () => {
        appContentContainer = document.getElementById(appContentContainerId);
        if (!appContentContainer) {
            console.error('Router: App content container not found:', appContentContainerId);
            return;
        }
        // Listen to view changes from State to potentially trigger rendering
        State.subscribe('viewChanged', (newView) => {
            // This is one way to link state change to route action.
            // navigateTo might be called directly too.
            // If navigateTo itself sets the state, this could be redundant or circular.
            // For now, assume navigateTo is the primary way to change views.
        });
    };

    /**
     * Navigates to a specific view/route with optional data
     * Called throughout the application to change views (e.g., from Home.render, PlayerComponent.render)
     * Updates state and renders the appropriate component
     */
    const navigateTo = (viewName, data = null) => {
        if (routes[viewName]) {
            State.setCurrentView(viewName); // Update state first
            console.log(`Routing to ${viewName} with data:`, data);
            if (!appContentContainer) { // Re-check container
                init(); // Attempt to re-init if container was missing (e.g., after async load)
                if(!appContentContainer) {
                     console.error("App content container still missing. Cannot navigate.");
                     return;
                }
            }
            
            routes[viewName](data); // Execute the route function to render the view
        } else {
            console.error(`Router: Route "${viewName}" not found.`);
            // Fallback to home or show a 404-like message in app-content
            if (appContentContainer) {
                appContentContainer.innerHTML = `<h2>Error: Page not found (${viewName})</h2>`;
            }
            State.setCurrentView('error'); // Or some error view
        }
    };

    return {
        init,
        navigateTo
    };
})();
