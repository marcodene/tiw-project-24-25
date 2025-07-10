# Music Playlist Manager

*Progetto del corso "Tecnologie Informatiche per il Web" - Politecnico di Milano*

## 🎵 Descrizione del Progetto

Applicazione web per la gestione di playlist musicali personali, sviluppata in due versioni architetturali diverse per dimostrare l'evoluzione delle tecnologie web moderne.

### Funzionalità Principali

- **Gestione Utenti**: Registrazione, login e gestione del profilo personale
- **Gestione Brani**: Upload, organizzazione e cancellazione di file musicali
- **Playlist Personalizzate**: Creazione, modifica e gestione di playlist su misura
- **Player Integrato**: Riproduzione audio con controlli avanzati
- **Riorganizzazione**: Drag & drop per riordinare i brani nelle playlist
- **Gestione File**: Upload di copertine e file audio con validazione

## 🏗️ Architettura del Progetto

### Versione Pure HTML (Tradizionale)
- **Paradigma**: Server-side rendering con navigazione multi-pagina
- **Template Engine**: Thymeleaf per la generazione dinamica delle pagine
- **Comunicazione**: Form-based con redirect/forward pattern
- **Autenticazione**: Session-based con servlet filters

### Versione RIA (Rich Internet Application)
- **Paradigma**: Single Page Application (SPA) con JavaScript
- **API**: RESTful endpoints per comunicazione asincrona
- **Routing**: Client-side routing con History API
- **Autenticazione**: Stateless con token validation

## 🛠️ Tecnologie Utilizzate

### Backend
- **Java 21** - Linguaggio di programmazione
- **Servlet API 4.0** - Framework web per Java
- **Apache Tomcat 10.1** - Application server
- **MySQL 8.0** - Database relazionale
- **Thymeleaf 3.1** - Template engine (versione Pure HTML)
- **Gson 2.8** - Serializzazione JSON (versione RIA)

### Frontend
- **HTML5** - Struttura delle pagine
- **CSS3** - Styling e responsive design
- **JavaScript (ES6+)** - Logica client-side (versione RIA)
- **Vanilla JavaScript** - Nessun framework esterno

### Database
- **MySQL** - Database principale
- **Schema Relazionale** - Tabelle User, Song, Playlist, Genre
- **Constraint di Integrità** - Foreign keys con CASCADE
- **Indici Ottimizzati** - Per query performance

## 🚀 Installazione e Setup

### Prerequisiti
- **Java 21** o superiore
- **Apache Tomcat 10.1** o superiore
- **MySQL Server 8.0** o superiore
- **IDE** (Eclipse, IntelliJ IDEA, VS Code)

### Configurazione Database

1. **Creazione Database**:
```sql
CREATE DATABASE db_progetto2425;
USE db_progetto2425;
```

2. **Esecuzione Schema**:
```bash
mysql -u root -p db_progetto2425 < database/schema.sql
```

### Configurazione Applicazione

⚠️ **IMPORTANTE**: Prima di eseguire l'applicazione, è necessario configurare i file di proprietà con le proprie credenziali e percorsi.

#### File di Configurazione Richiesti

I seguenti file devono essere creati manualmente (non sono inclusi nel repository per motivi di sicurezza):

**1. Database Configuration**
- **Versione Pure HTML**: `pure-html-version/src/main/webapp/WEB-INF/database.properties`
- **Versione RIA**: `ria-version/src/main/webapp/WEB-INF/database.properties`

```properties
# Database Connection Properties
dbDriver=com.mysql.cj.jdbc.Driver
dbUrl=jdbc:mysql://localhost:3306/db_progetto2425?serverTimezone=UTC
dbUser=root
dbPassword=your_database_password
```

**2. File Storage Configuration**
- **Versione Pure HTML**: `pure-html-version/src/main/webapp/WEB-INF/file_storage.properties`
- **Versione RIA**: `ria-version/src/main/webapp/WEB-INF/file_storage.properties`

```properties
# File Storage Paths
baseStoragePath=/path/to/your/uploads/directory
coverImagesDir=covers
audioFilesDir=songs
```

#### Nota sulla Sicurezza
- I file `.properties` sono esclusi dal controllo versione tramite `.gitignore`
- **NON committare mai** questi file con credenziali reali
- Utilizzare credenziali di database dedicate per lo sviluppo
- Assicurarsi che le directory di upload abbiano i permessi corretti

### Deployment

#### Versione Pure HTML
1. Compilare il progetto Java
2. Creare il file WAR
3. Deployare su Tomcat
4. Accedere a `http://localhost:8080/progetto-tiw-24-25`

#### Versione RIA
1. Compilare il progetto Java
2. Creare il file WAR
3. Deployare su Tomcat
4. Accedere a `http://localhost:8080/progetto-tiw-24-25-ria`

## 📁 Struttura del Progetto

```
music-playlist-manager/
├── pure-html-version/          # Versione tradizionale
│   ├── src/main/java/          # Servlet e logica business
│   └── src/main/webapp/        # Template Thymeleaf e CSS
├── ria-version/                # Versione SPA
│   ├── src/main/java/          # API REST controllers
│   └── src/main/webapp/        # JavaScript, HTML, CSS
├── shared/                     # Componenti condivisi
│   ├── java/beans/             # Modelli dati
│   ├── java/dao/               # Database access objects
│   └── java/utils/             # Utility classes
├── database/                   # Scripts SQL
├── docs/                       # Documentazione tecnica
└── sample-uploads/             # File di esempio
```

## 🔄 Differenze tra le Versioni

| Aspetto | Pure HTML | RIA |
|---------|-----------|-----|
| **Architettura** | Multi-page | Single-page |
| **Rendering** | Server-side | Client-side |
| **Comunicazione** | Form POST/GET | AJAX/Fetch API |
| **Navigazione** | Page reload | Dynamic routing |
| **Stato** | Session-based | JavaScript state |
| **Performance** | Server load | Client load |
| **SEO** | Ottimizzato | Richiede configurazione |

## 🎯 Funzionalità Dettagliate

### Gestione Utenti
- **Registrazione**: Validazione lato server e client
- **Login**: Autenticazione con sessione/token
- **Profilo**: Visualizzazione e modifica dati personali
- **Sicurezza**: Password hashing e validation

### Gestione Brani
- **Upload**: Supporto MP3 con validazione MIME type
- **Metadati**: Titolo, artista, genere, durata
- **Copertine**: Upload immagini con resize automatico
- **Organizzazione**: Filtri per genere e ricerca

### Playlist Management
- **Creazione**: Nome personalizzato e descrizione
- **Modifica**: Aggiunta/rimozione brani
- **Riordinamento**: Drag & drop interface (RIA)
- **Condivisione**: Gestione privacy e accesso

### Player Audio
- **Controlli**: Play, pause, stop, volume
- **Playlist**: Riproduzione sequenziale
- **Visualizzazione**: Waveform e progress bar
- **Modalità**: Random, repeat, single

## 🧪 Testing

### Test Unitari
- **DAO Testing**: Validazione accesso database
- **Controller Testing**: Test logica business
- **Integration Testing**: Test end-to-end

### Test Manuali
- **Functional Testing**: Ogni funzionalità
- **Security Testing**: Validazione input
- **Performance Testing**: Carico e stress
- **Usability Testing**: Esperienza utente

## 🔒 Sicurezza

### Misure Implementate
- **Input Validation**: Sanitizzazione dati utente
- **SQL Injection Prevention**: Prepared statements
- **XSS Protection**: Output encoding
- **CSRF Protection**: Token validation
- **File Upload Security**: Validazione MIME type
- **Authentication**: Session management sicuro

## 📊 Database Schema

### Tabelle Principali
- **User**: Informazioni utente e credenziali
- **Song**: Metadati brani e file paths
- **Playlist**: Playlist utente
- **Genre**: Generi musicali
- **PlaylistSong**: Relazione many-to-many

### Relazioni
- User 1:N Playlist
- User 1:N Song
- Playlist M:N Song (through PlaylistSong)
- Song N:1 Genre

## 🚀 Deployment in Produzione

### Ottimizzazioni
- **Database**: Connection pooling e query optimization
- **File Storage**: CDN per file statici
- **Caching**: Redis per sessioni e cache
- **Load Balancing**: Nginx reverse proxy
- **SSL/TLS**: Certificati HTTPS

### Monitoraggio
- **Logging**: SLF4J con Logback
- **Metrics**: JMX monitoring
- **Health Checks**: Endpoint diagnostici
- **Error Tracking**: Centralized error logging

## 👥 Sviluppatori

**Marco De Negri** - Studente Politecnico di Milano  
Corso: Tecnologie Informatiche per il Web  
Anno Accademico: 2024-2025

## 📝 Note di Sviluppo

- **Versione Pure HTML**: Implementata per prima seguendo paradigmi tradizionali
- **Versione RIA**: Evoluzione moderna con JavaScript e API REST
- **Codice Condiviso**: Beans, DAO e utility riutilizzati tra versioni
- **Best Practices**: Separation of concerns, clean code, documentation

## 🔗 Risorse Utili

- [Documentazione Servlet API](https://docs.oracle.com/javaee/7/api/javax/servlet/package-summary.html)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [Apache Tomcat Documentation](https://tomcat.apache.org/tomcat-10.1-doc/)

## 📄 Licenza

Questo progetto è sviluppato per scopi educativi nell'ambito del corso "Tecnologie Informatiche per il Web" del Politecnico di Milano.