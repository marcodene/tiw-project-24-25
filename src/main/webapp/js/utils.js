const baseURL = '/progetto-tiw-24-25-RIA2.0';

/**
 * Makes HTTP requests to the server with support for different data types
 * Called throughout the application for all API communication
 * Handles JSON, FormData, form elements, and null data with appropriate content types
 */
function makeCall(method, url, data, cback, reset = true) {
    var req = new XMLHttpRequest();
    req.onreadystatechange = function() {
        if (typeof cback === 'function') {
            cback(req);
        }
    };
    req.open(method, baseURL + url);
    req.setRequestHeader("X-Requested-With", "XMLHttpRequest");

    if (typeof data === 'string') {
        // JSON data - used for structured data sent to APIs
        console.log("\nSending JSON: ", data);
        req.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        req.send(data);
    } else if (data instanceof FormData) {
        // FormData - used for file uploads (e.g. song upload)
        console.log("\nSending FormData: ", data);
        req.send(data);
    } else if (data === null || typeof data === 'undefined') {
        // Null/undefined - used for GET requests without body
        console.log("\nSending null: ", data);
        req.send();
    } else if (data.tagName && data.tagName.toLowerCase() === 'form') {
        // HTML Form - used for login/register with standard form data
        console.log("\nSending form: ", data);
        req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        
        const params = new URLSearchParams();
        for (let i = 0; i < data.elements.length; i++) {
            const elem = data.elements[i];
            if (elem.name && elem.value && elem.type !== 'file') {
                params.append(elem.name, elem.value);
            }
        }
        
        req.send(params.toString());
        
        if (reset && typeof data.reset === 'function') {
            data.reset();
        }
    } else {
        // HTMLFormElement fallback - for form elements passed directly
        console.log("\nSending HTMLFormElement: ", data);
        try {
            const formData = new FormData(data);
            req.send(formData);
            if (reset && data && typeof data.reset === 'function') {
                data.reset();
            }
        } catch (e) {
            console.error("makeCall: Unsupported data type:", data, e);
            req.send();
        }
    }
}

// Session Management Utility
const SessionManager = {
    /**
     * Stores user data in sessionStorage
     * Called by Auth module during login/registration to persist user session
     * Serializes user object to JSON and handles storage errors
     */
    setUser(userData) {
        try {
            sessionStorage.setItem('user', JSON.stringify(userData));
            console.log('✅ User data saved to session');
        } catch (e) {
            console.error('❌ Failed to save user data:', e);
        }
    },

    /**
     * Retrieves user data from sessionStorage
     * Called by State.getCurrentUser() and Auth module to check existing sessions
     * Parses JSON data and handles corrupted data by clearing session
     */
    getUser() {
        try {
            const stored = sessionStorage.getItem('user');
            return stored ? JSON.parse(stored) : null;
        } catch (e) {
            console.error('❌ Corrupted user data, clearing session');
            this.clearUser();
            return null;
        }
    },

    /**
     * Removes user data from sessionStorage
     * Called during logout process to clear user session
     * Ensures clean logout by removing stored authentication data
     */
    clearUser() {
        sessionStorage.removeItem('user');
        console.log('🗑️ User session cleared');
    },

    /**
     * Checks if stored user data is valid and complete
     * Called to validate session before allowing access to protected features
     * Returns true if user has required properties (username, id)
     */
    hasValidUser() {
        const user = this.getUser();
        return user && user.username && user.id;
    }
};
