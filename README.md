# Music Playlist Manager

*Web Technologies Project - Politecnico di Milano*

## 🎵 Project Description

A web application for managing personal music playlists, developed in two different architectural versions to demonstrate the evolution of modern web technologies.

### Main Features

- **User Management**: Registration, login, and personal profile management
- **Song Management**: Upload, organize, and delete music files
- **Custom Playlists**: Create, edit, and manage personalized playlists
- **Integrated Player**: Audio playback with advanced controls
- **Drag & Drop Reordering**: Reorder songs in playlists (RIA version)
- **File Management**: Upload covers and audio files with validation

## 🏗️ Project Architecture

### Pure HTML Version (Traditional)
- **Paradigm**: Server-side rendering with multi-page navigation
- **Template Engine**: Thymeleaf for dynamic page generation
- **Communication**: Form-based with redirect/forward pattern
- **Authentication**: Session-based with servlet filters

### RIA Version (Rich Internet Application)
- **Paradigm**: Single Page Application (SPA) with JavaScript
- **API**: RESTful endpoints for asynchronous communication
- **Routing**: Client-side routing with History API
- **Authentication**: Stateless with token validation

## 🛠️ Technologies Used

### Backend
- **Java 21** - Programming language
- **Servlet API 4.0** - Web framework for Java
- **Apache Tomcat 10.1** - Application server
- **MySQL 8.0** - Relational database
- **Thymeleaf 3.1** - Template engine (Pure HTML version)
- **Gson 2.8** - JSON serialization (RIA version)

### Frontend
- **HTML5** - Page structure
- **CSS3** - Styling and responsive design
- **JavaScript (ES6+)** - Client-side logic (RIA version)
- **Vanilla JavaScript** - No external frameworks

### Database
- **MySQL** - Main database
- **Relational Schema** - User, Song, Playlist, Genre tables
- **Integrity Constraints** - Foreign keys with CASCADE
- **Optimized Indexes** - For query performance

## 🚀 Installation and Setup

### Prerequisites
- **Java 21** or higher
- **Apache Tomcat 10.1** or higher
- **MySQL Server 8.0** or higher
- **IDE** (Eclipse, IntelliJ IDEA, VS Code)

### Database Configuration

1. **Create Database**:
```sql
CREATE DATABASE db_progetto2425;
USE db_progetto2425;
```

2. **Execute Schema**:
```bash
mysql -u root -p db_progetto2425 < database/schema.sql
```

**Note**: The `database/schema.sql` file contains the complete schema compatible with both versions, including the `customOrder` field necessary for the RIA version's reordering functionality.

### Application Configuration

⚠️ **IMPORTANT**: Before running the application, you must configure the properties files with your credentials and paths.

#### Required Configuration Files

The following files must be created manually (not included in the repository for security reasons):

**1. Database Configuration**
- **Pure HTML Version**: `pure-html-version/src/main/webapp/WEB-INF/database.properties`
- **RIA Version**: `ria-version/src/main/webapp/WEB-INF/database.properties`

```properties
# Database Connection Properties
dbDriver=com.mysql.cj.jdbc.Driver
dbUrl=jdbc:mysql://localhost:3306/db_progetto2425?serverTimezone=UTC
dbUser=root
dbPassword=your_database_password
```

**2. File Storage Configuration**
- **Pure HTML Version**: `pure-html-version/src/main/webapp/WEB-INF/file_storage.properties`
- **RIA Version**: `ria-version/src/main/webapp/WEB-INF/file_storage.properties`

```properties
# File Storage Paths
baseStoragePath=/path/to/your/uploads/directory
coverImagesDir=covers
audioFilesDir=songs
```

#### Security Note
- The `.properties` files are excluded from version control via `.gitignore`
- **NEVER commit** these files with real credentials
- Use dedicated database credentials for development
- Ensure upload directories have correct permissions

### Deployment

#### Pure HTML Version
1. Compile the Java project
2. Create the WAR file
3. Deploy to Tomcat
4. Access at `http://localhost:8080/progetto-tiw-24-25-pureHTML`

#### RIA Version
1. Compile the Java project
2. Create the WAR file
3. Deploy to Tomcat
4. Access at `http://localhost:8080/progetto-tiw-24-25-RIA`

## 📁 Project Structure

```
tiw-project-24-25/
├── pure-html-version/          # Traditional version
│   ├── src/main/java/          # Servlets and business logic
│   └── src/main/webapp/        # Thymeleaf templates and CSS
├── ria-version/                # SPA version
│   ├── src/main/java/          # REST API controllers
│   └── src/main/webapp/        # JavaScript, HTML, CSS
├── database/                   # SQL scripts
│   └── schema.sql              # Unified database schema
├── docs/                       # Technical documentation
│   ├── requirements.pdf        # Project requirements
│   └── presentation.pdf        # Project presentation
└── sample-uploads/             # Example files
    ├── covers/                 # Sample album covers
    └── songs/                  # Sample audio files
```

## 🔄 Version Differences

| Aspect | Pure HTML | RIA |
|--------|-----------|-----|
| **Architecture** | Multi-page application | Single Page Application |
| **Navigation** | Page reload with server redirect | Asynchronous without page refresh |
| **Rendering** | Server-side with Thymeleaf | Client-side with JavaScript |
| **Communication** | Form POST/GET | RESTful API endpoints |
| **Playlist Ordering** | Default alphabetical | Drag & drop with custom ordering |

## 🎯 Main Features

### Home Page
- List of playlists ordered by creation date (descending)
- Form to upload new songs with metadata (title, album, artist, year, genre)
- Form to create new playlists
- Song list ordered by artist and album release year

### Playlist Page
- Grid view of songs (5 columns display)
- Navigation between song groups (PREVIOUS/NEXT)
- Form to add songs to playlist
- Custom ordering with drag & drop (RIA version only)

### Player Page
- Song details display
- Audio player for playback
- Delete song functionality

### Account Management
- User registration and login
- Personal account page
- Delete operations (songs, playlists, account)
- Secure logout with session invalidation

### Additional Features
- Private playlists (not shared between users)
- Post-Redirect-Get pattern to prevent double submissions
- Ownership validation for all operations
- 5 songs per page pagination in playlists

## 🔒 Security

### Implemented Measures
- **Input Validation**: User data sanitization
- **SQL Injection Prevention**: Prepared statements
- **XSS Protection**: Output encoding
- **CSRF Protection**: Token validation
- **File Upload Security**: MIME type validation
- **Authentication**: Secure session management

## 📊 Database Schema

### Main Tables
- **User**: User information and credentials
- **Song**: Track metadata and file paths
- **Playlist**: User playlists
- **Genre**: Music genres
- **PlaylistSong**: Many-to-many relationship

### Relationships
- User 1:N Playlist
- User 1:N Song
- Playlist M:N Song (through PlaylistSong)
- Song N:1 Genre

## 👥 Developers

**Marco De Negri** - Politecnico di Milano Student  
**Alice Berta** - Politecnico di Milano Student  

Course: Web Technologies (Tecnologie Informatiche per il Web)  
Academic Year: 2024-2025

## 📝 Development Notes

- **Pure HTML Version**: Implemented first following traditional paradigms
- **RIA Version**: Modern evolution with JavaScript and REST APIs
- **Shared Code**: Beans, DAOs, and utilities reused between versions
- **Best Practices**: Separation of concerns, clean code, documentation

## 📄 License

This project is developed for educational purposes as part of the "Web Technologies" course at Politecnico di Milano.