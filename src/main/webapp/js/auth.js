const Auth = (() => {
	const authContainerId = 'auth-container';
	const appContainerId = 'app-container';
	const loginFormContainerId = 'login-form-container';
	const registerFormContainerId = 'register-form-container';
	const errorMessageElementId = 'auth-error-message';

	let loginFormContainer, registerFormContainer, authContainer, appContainer, errorMessageElement;

	const init = () => {
		loginFormContainer = document.getElementById(loginFormContainerId);
		registerFormContainer = document.getElementById(registerFormContainerId);
		authContainer = document.getElementById(authContainerId);
		appContainer = document.getElementById(appContainerId);
		errorMessageElement = document.getElementById(errorMessageElementId);

		// Check initial auth status (e.g., via an API call)
		checkSessionAndSetup();
	};

	const checkSessionAndSetup = () => {
	    makeCall('GET', '/api/checkAuth', null, (req) => {
	        if (req.readyState === XMLHttpRequest.DONE) {
	            try {
	                const response = JSON.parse(req.responseText);
	                
	                if (req.status === 200 && response.status === 'success') {
	                    // Sessione valida trovata
	                    console.log("User session found, initializing app.");
	                    
	                    // Opzionale: salva in sessionStorage per avere una copia locale
	                    sessionStorage.setItem('user', JSON.stringify(response.data));
	                    
	                    showApp();
	                    App.init(response.data); // Inizializza l'applicazione con i dati utente
	                } else {
	                    // Nessuna sessione trovata o errore
	                    console.log("No active session, showing login form.");
	                    showLoginForm();
	                    setupEventListeners();
	                }
	            } catch (e) {
	                // Errore nel parsing della risposta JSON
	                console.error("Error parsing authentication response:", e);
	                showLoginForm();
	                setupEventListeners();
	            }
	        }
	    });
	};

	const showLoginForm = () => {
		if (!loginFormContainer || !registerFormContainer || !authContainer || !appContainer) {
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
                <p>Non hai un account? <a href="#" id="showRegister">Registrati</a></p>
            </form>
        `;
		loginFormContainer.style.display = 'block';
		registerFormContainer.style.display = 'none';
		authContainer.style.display = 'block';
		appContainer.style.display = 'none';
		clearErrorMessage();
		document.getElementById('loginForm').addEventListener('submit', handleLogin);
		document.getElementById('showRegister').addEventListener('click', (e) => { e.preventDefault(); showRegisterForm(); });
	};

	const showRegisterForm = () => {
		if (!loginFormContainer || !registerFormContainer) {
			console.error('Auth containers not found'); return;
		}
		registerFormContainer.innerHTML = `
            <form id="registerForm">
                <h3>Registrazione</h3>
                <div>
                    <label for="registerUsername">Username:</label>
                    <input type="text" id="registerUsername" name="username" required>
                </div>
                <div>
                    <label for="registerName">Nome:</label>
                    <input type="text" id="registerName" name="name" required>
                </div>
                <div>
                    <label for="registerSurname">Cognome:</label>
                    <input type="text" id="registerSurname" name="surname" required>
                </div>
                <div>
                    <label for="registerPassword">Password:</label>
                    <input type="password" id="registerPassword" name="password" required>
                </div>
                <div>
                    <label for="registerConfirmPassword">Conferma Password:</label>
                    <input type="password" id="registerConfirmPassword" name="confirmPassword" required>
                </div>
                <button type="submit">Registrati</button>
                <p>Hai già un account? <a href="#" id="showLogin">Accedi</a></p>
            </form>
        `;
		loginFormContainer.style.display = 'none';
		registerFormContainer.style.display = 'block';
		clearErrorMessage();
		document.getElementById('registerForm').addEventListener('submit', handleRegister);
		document.getElementById('showLogin').addEventListener('click', (e) => { e.preventDefault(); showLoginForm(); });
	};

	const handleLogin = (event) => {
		event.preventDefault();
		const form = event.target;
		if (!validateForm(form)) return;

		makeCall('POST', '/api/login', form, (req) => {
			if (req.readyState === XMLHttpRequest.DONE) {
				const response = JSON.parse(req.responseText);
				if (req.status === 200 && response.status === 'success') {
					sessionStorage.setItem('user', JSON.stringify(response.data));
					displaySuccessMessage("Login successful! Redirecting...");
					setTimeout(() => {
						showApp();
						App.init(response.data);
					}, 1000);
				} else {
					const message = response.message || "Login fallito.";
					displayErrorMessage(message);
				}
			}
		});
		
	};

	const handleRegister = (event) => {
		event.preventDefault();
		const form = event.target;
		if (!validateForm(form)) return;

		const password = form.password.value;
		const confirmPassword = form.confirmPassword.value;

		if (password !== confirmPassword) {
			displayErrorMessage("Le password non coincidono.");
			return;
		}
		
		displaySuccessMessage("Registrazione in corso...");

		makeCall('POST', '/api/register', form, (req) => {
			if (req.readyState === XMLHttpRequest.DONE) {
				const response = JSON.parse(req.responseText);
				if (req.status === 201 && response.status === 'success') { // Assuming 201 Created for new user
					sessionStorage.setItem('user', JSON.stringify(response.data));
					 displaySuccessMessage("Registrazione completata! Redirecting...");
					setTimeout(() => {
						showApp();
						App.init(response.data);
					}, 1000);
				} else {
					let message = response.message || "Registrazione fallita.";
					if(response.errors){
						message += " Errors: " + Object.values(response.errors).join(', ');
					}
					displayErrorMessage(message);
				}
			}
		});
		
	};

	const handleLogout = () => {
		makeCall('POST', '/api/logout', null, (req) => {
			if (req.readyState === XMLHttpRequest.DONE) {
				sessionStorage.removeItem('user');
				if (req.status === 200) { // Assuming 200 for successful logout
					displaySuccessMessage("Logout successful!");
					 setTimeout(() => {
						showLoginForm();
						if (typeof App !== 'undefined' && App.reset) {
							App.reset();
						}
					}, 1000);
				} else {
					displayErrorMessage("Logout fallito. Riprova.");
				}
			}
		});
		
	};

	const validateForm = (form) => {
		for (let element of form.elements) {
			if (element.required && !element.value) {
				displayErrorMessage(`${element.labels[0] ? element.labels[0].innerText : element.name} è obbligatorio.`);
				return false;
			}
		}
		clearErrorMessage();
		return true;
	};

	const showApp = () => {
		if (!authContainer || !appContainer) {
			console.error('Auth or App container not found'); return;
		}
		authContainer.style.display = 'none';
		appContainer.style.display = 'block';
		clearErrorMessage();
	};

	const setupEventListeners = () => {
		// Event listeners for forms are set up when they are shown
		// Add listener for logout button if it's part of the main app structure visible after login
		const logoutButton = document.getElementById('logoutButton');
		if (logoutButton) {
			logoutButton.addEventListener('click', handleLogout);
		} else {
			// If app structure is dynamic, logout button listener might need to be added in App.init()
			console.warn("Logout button not found during Auth.init. Ensure it's available or attached later.");
		}
	};

	const displayErrorMessage = (message) => {
		if (!errorMessageElement) return;
		errorMessageElement.textContent = message;
		errorMessageElement.style.display = 'block';
		errorMessageElement.className = 'error-message';
	};

	const displaySuccessMessage = (message) => {
		if (!errorMessageElement) return;
		errorMessageElement.textContent = message;
		errorMessageElement.style.display = 'block';
		errorMessageElement.className = 'success-message'; // Assuming you have a CSS class for success
	};

	const clearErrorMessage = () => {
		if (!errorMessageElement) return;
		errorMessageElement.textContent = '';
		errorMessageElement.style.display = 'none';
	};

	return {
		init,
		showLoginForm,
		showRegisterForm,
		handleLogout, // Expose logout to be callable from App.js if needed
		checkSessionAndSetup // Expose for potential re-check
	};
})();
