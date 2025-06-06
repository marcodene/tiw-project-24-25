const baseURL = '/progetto-tiw-24-25-RIA2.0';

/**
 * Makes HTTP requests to the server with different data types support
 * Called throughout the application for API communication
 * Handles form data, JSON, and file uploads with proper content types
 */
function makeCall(method, url, data, cback, reset = true) {
    var req = new XMLHttpRequest();
    req.onreadystatechange = function() {
        // It's good practice to check if cback is a function before calling
        if (typeof cback === 'function') {
            cback(req);
        }
    };
    req.open(method, baseURL + url);
    req.setRequestHeader("X-Requested-With", "XMLHttpRequest"); // Common header for AJAX

    if (typeof data === 'string') {
        // Assuming string data is JSON
        req.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        req.send(data);
        // 'reset' is not applicable to string data, so we don't attempt form.reset()
    } else if (data instanceof FormData) {
        // FormData handles its own Content-Type (multipart/form-data)
        req.send(data);
        if (reset && data.hasOwnProperty('reset') && typeof data.reset === 'function') { 
            // This check is actually for form elements, not FormData directly.
            // The original 'formElement' was the HTMLFormElement.
            // If 'data' is FormData, it was created from a form. The form should be passed for reset.
            // This part of the original logic might need rethinking if 'data' is FormData.
            // For now, we assume 'reset' applies to an HTMLFormElement passed as 'data' conceptually.
            // The components are currently passing FormData, not the form element itself to makeCall.
            // Let's adjust: if 'reset' is true and 'data' came from a form, the component should reset the form.
            // The makeCall function itself won't reset a FormData object.
            // The components were calling form.reset() themselves after makeCall, which is fine.
            // So, the 'reset' parameter for FormData here is less relevant for FormData itself.
            // The original: if (formElement !== null && reset === true) { formElement.reset(); }
            // If data is FormData, we can't reset it here. Components should handle form reset.
        }
    } else if (data === null || typeof data === 'undefined') { // For GET or no body
        req.send();
	} else if (data.tagName && data.tagName.toLowerCase() === 'form') {
        // Form HTML - uses application/x-www-form-urlencoded
        req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        
        // Convert form fields to URL-encoded string
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
        // Potentially other data types, or an actual HTMLFormElement
        // If 'data' is an HTMLFormElement, we can use new FormData(data)
        // For simplicity, current components build FormData themselves or pass string/null.
        // This path would be for if components passed the HTMLFormElement directly.
        try {
            const formData = new FormData(data); // data is an HTMLFormElement
            req.send(formData);
            if (reset && data && typeof data.reset === 'function') {
                data.reset();
            }
        } catch (e) {
            console.error("makeCall: Data is of unexpected type or not a valid form for FormData:", data, e);
            req.send(); // Send empty for safety if data type is unhandled but method expects body
        }
    }
    // The original reset logic was:
    // if (formElement !== null && reset === true) { formElement.reset(); }
    // This needs to be handled by the calling component if it passes FormData.
    // If it passes the HTMLFormElement, the above try-catch for FormData handles it.
    // The current components (Home.js) using FormData call form.reset() themselves, which is the correct pattern.
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
