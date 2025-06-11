const Auth = (() => {
	const authContainerId = 'auth-container';
	const appContainerId = 'app-container';
	const loginFormContainerId = 'login-form-container';
	const registerFormContainerId = 'register-form-container';
	const errorMessageElementId = 'auth-error-message';

	let loginFormContainer, registerFormContainer, authContainer, appContainer, errorMessageElement;

	/**
	 * Displays the login form interface
	 * Called when no valid session exists or user needs to authenticate
	 * Creates login form HTML and sets up event listeners
	 */
	const showLoginForm = () => {
		if (!loginFormContainer || !registerFormContainer || !authContainer) {
			console.error('Auth containers not found'); return;
		}
		loginFormContainer.innerHTML = `
            <form id="loginForm">
                <h3>Login</h3>
                <div>
                    <label for="loginUsername">Username:</label>
                    <input type="text" id="loginUsername" name="username" required>
                </div>
                <div>
                    <label for="loginPassword">Password:</label>
                    <input type="password" id="loginPassword" name="password" required>
                </div>
                <button type="submit">Login</button>
                <p>Don't have an account? <a href="#" id="showRegister">Register</a></p>
            </form>
        `;
		loginFormContainer.style.display = 'block';
		registerFormContainer.style.display = 'none';
		authContainer.style.display = 'block';
		clearErrorMessage();
		document.getElementById('loginForm').addEventListener('submit', handleLogin);
		document.getElementById('showRegister').addEventListener('click', (e) => { e.preventDefault(); showRegisterForm(); });
	};

	/**
	 * Displays the user registration form interface
	 * Called when user clicks the "Register" link from the login form
	 * Creates registration form HTML with username, name, surname, password fields
	 */
	const showRegisterForm = () => {
		if (!loginFormContainer || !registerFormContainer) {
			console.error('Auth containers not found'); return;
		}
		registerFormContainer.innerHTML = `
            <form id="registerForm">
                <h3>Registration</h3>
                <div>
                    <label for="registerUsername">Username:</label>
                    <input type="text" id="registerUsername" name="username" required>
                </div>
                <div>
                    <label for="registerName">Name:</label>
                    <input type="text" id="registerName" name="name" required>
                </div>
                <div>
                    <label for="registerSurname">Surname:</label>
                    <input type="text" id="registerSurname" name="surname" required>
                </div>
                <div>
                    <label for="registerPassword">Password:</label>
                    <input type="password" id="registerPassword" name="password" required>
                </div>
                <div>
                    <label for="registerConfirmPassword">Confirm Password:</label>
                    <input type="password" id="registerConfirmPassword" name="confirmPassword" required>
                </div>
                <button type="submit">Register</button>
                <p>Already have an account? <a href="#" id="showLogin">Login</a></p>
            </form>
        `;
		loginFormContainer.style.display = 'none';
		registerFormContainer.style.display = 'block';
		clearErrorMessage();
		document.getElementById('registerForm').addEventListener('submit', handleRegister);
		document.getElementById('showLogin').addEventListener('click', (e) => { e.preventDefault(); showLoginForm(); });
	};

	/**
	 * Handles user login form submission
	 * Called when login form is submitted via event listener
	 * Validates form data, sends login request to server, and handles authentication response
	 */
	const handleLogin = (event) => {
		event.preventDefault();
		const form = event.target;
		if (!validateForm(form)) return;

		makeCall('POST', '/api/login', form, (req) => {
			if (req.readyState === XMLHttpRequest.DONE) {
				const response = JSON.parse(req.responseText);
				if (req.status === 200 && response.status === 'success') {
					SessionManager.setUser(response.data);
					displaySuccessMessage("Login successful! Redirecting...");
					setTimeout(() => {
						showApp();
					}, 1000);
				} else {
					const message = response.message || "Login failed.";
					displayErrorMessage(message);
				}
			}
		});
		
	};

	/**
	 * Handles user registration form submission
	 * Called when registration form is submitted via event listener
	 * Validates form data, checks password confirmation, and sends registration request to server
	 */
	const handleRegister = (event) => {
		event.preventDefault();
		const form = event.target;
		if (!validateForm(form)) return;

		const password = form.password.value;
		const confirmPassword = form.confirmPassword.value;

		if (password !== confirmPassword) {
			displayErrorMessage("Passwords do not match.");
			return;
		}
		
		displaySuccessMessage("Registration in progress...");

		makeCall('POST', '/api/register', form, (req) => {
			if (req.readyState === XMLHttpRequest.DONE) {
				const response = JSON.parse(req.responseText);
				if (req.status === 201 && response.status === 'success') { // Assuming 201 Created for new user
					sessionStorage.setItem('user', JSON.stringify(response.data));
					 displaySuccessMessage("Registration completed! Redirecting...");
					setTimeout(() => {
						showApp();
					}, 1000);
				} else {
					let message = response.message || "Registration failed.";
					if(response.errors){
						message += " Errors: " + Object.values(response.errors).join(', ');
					}
					displayErrorMessage(message);
				}
			}
		});
		
	};

	/**
	 * Handles user logout process
	 * Called when logout button is clicked or from external components
	 * Sends logout request to server, clears session data, and redirects to login page
	 */
	const handleLogout = () => {
		makeCall('POST', '/api/logout', null, (req) => {
			if (req.readyState === XMLHttpRequest.DONE) {
				SessionManager.clearUser();
				if (req.status === 200) { // Assuming 200 for successful logout
					// Clean up application state before redirect
					if (typeof App !== 'undefined' && App.reset) {
						App.reset();
					}
					// Redirect to login page
					window.location.href = 'login.html';
				} else {
					displayErrorMessage("Logout failed. Please try again.");
				}
			}
		});
		
	};

	/**
	 * Validates form data before submission
	 * Called by handleLogin() and handleRegister() to ensure required fields are filled
	 * Checks all required form elements and displays error messages for missing fields
	 */
	const validateForm = (form) => {
		for (let element of form.elements) {
			if (element.required && !element.value) {
				displayErrorMessage(`${element.labels[0] ? element.labels[0].innerText : element.name} is required.`);
				return false;
			}
		}
		clearErrorMessage();
		return true;
	};

	/**
	 * Redirects user to the main application after successful authentication
	 * Called after successful login or registration to navigate to the home page
	 * Performs a full page redirect to initialize the main application interface
	 */
	const showApp = () => {
		// Redirect to home page after successful authentication
		window.location.href = './';
	};

	/**
	 * Displays error messages to the user in the authentication interface
	 * Called when form validation fails or server returns error responses
	 * Shows red-styled error message in the designated error message element
	 */
	const displayErrorMessage = (message) => {
		if (!errorMessageElement) return;
		errorMessageElement.textContent = message;
		errorMessageElement.style.display = 'block';
		errorMessageElement.className = 'error-message';
	};

	/**
	 * Displays success messages to the user in the authentication interface
	 * Called when login/registration is successful or operations complete successfully
	 * Shows green-styled success message in the designated error message element
	 */
	const displaySuccessMessage = (message) => {
		if (!errorMessageElement) return;
		errorMessageElement.textContent = message;
		errorMessageElement.style.display = 'block';
		errorMessageElement.className = 'success-message';
	};

	/**
	 * Clears any displayed error or success messages from the interface
	 * Called when switching between forms or before new operations to reset message state
	 * Hides message element and clears its content
	 */
	const clearErrorMessage = () => {
		if (!errorMessageElement) return;
		errorMessageElement.textContent = '';
		errorMessageElement.style.display = 'none';
	};

	/**
	 * Synchronizes local session data with server-side session state
	 * Called by session checking methods to ensure consistency between client and server
	 * Returns Promise that resolves with user data if valid session exists, null otherwise
	 */
	const syncSessionWithServer = () => {
		return new Promise((resolve) => {
			makeCall('GET', '/api/checkAuth', null, (req) => {
				if (req.readyState === XMLHttpRequest.DONE) {
					const serverUser = parseServerResponse(req);
					
					if (serverUser) {
						//SessionManager.setUser(serverUser);
						resolve(serverUser);
					} else {
						SessionManager.clearUser();
						resolve(null);
					}
				}
			});
		});
	};

	const parseServerResponse = (req) => {
		try {
			const response = JSON.parse(req.responseText);
			return (req.status === 200 && response.status === 'success') ? response.data : null;
		} catch (error) {
			console.error('❌ Session validation failed:', error);
			return null;
		}
	};

	/**
	 * Initializes authentication module specifically for the login page
	 * Called when login.html page loads to set up authentication interface
	 * Sets up DOM references and checks if user is already authenticated for redirect
	 */
	const initLoginPage = () => {
		loginFormContainer = document.getElementById(loginFormContainerId);
		registerFormContainer = document.getElementById(registerFormContainerId);
		authContainer = document.getElementById(authContainerId);
		errorMessageElement = document.getElementById(errorMessageElementId);

		// Check if already authenticated and redirect
		checkSessionAndRedirect();
	};

	/**
	 * Checks current session status and redirects if user is already authenticated
	 * Called by initLoginPage() to prevent authenticated users from seeing login form
	 * Redirects to home page if valid session exists, otherwise shows login form
	 */
	const checkSessionAndRedirect = () => {
		syncSessionWithServer().then(userData => {
			if (userData) {
				// User already authenticated, redirect to home
				console.log("User already authenticated, redirecting to home");
				window.location.href = './';
			} else {
				// No valid session, show login form
				console.log("No active session, showing login form.");
				showLoginForm();
			}
		}).catch(error => {
			// Error during session check, show login form
			console.log("Error parsing authentication response, showing login form.", error);
			showLoginForm();
		});
	};

	/**
	 * Validates user session specifically for the home page access
	 * Called when home page loads to ensure user is authenticated before showing app
	 * Initializes main application if valid session exists, otherwise redirects to login
	 */
	const checkSessionForHomePage = () => {
		console.log("🔍 checkSessionForHomePage: Checking session...");
		
		syncSessionWithServer().then(userData => {
			if (userData) {
				console.log("✅ checkSessionForHomePage: Valid session, initializing app");
				App.init(userData);
			} else {
				console.log("❌ checkSessionForHomePage: No valid session, redirecting to login");
				window.location.href = 'login.html';
			}
		}).catch(error => {
			console.error("❌ checkSessionForHomePage: Error during session sync", error);
			window.location.href = 'login.html';
		});
	};

	return {
		initLoginPage,
		showLoginForm,
		showRegisterForm,
		handleLogout, // Expose logout to be callable from App.js if needed
		checkSessionForHomePage
	};
})();
