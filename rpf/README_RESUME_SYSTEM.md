# Resume Management System

A complete Resume Management System built with **Java 17**, **Spring Boot 3**, **Apache Tika**, and **MongoDB** that allows you to upload, convert, view, and download resume documents.

## Features

✅ **Document Upload** - Upload resumes in various formats (PDF, DOC, DOCX, TXT, RTF, etc.)  
✅ **Apache Tika Conversion** - Automatically convert any document format to HTML  
✅ **MongoDB Storage** - Store converted HTML and original document data  
✅ **Resume Viewer** - View converted resumes in a beautiful web interface  
✅ **Download Original** - Download the original uploaded document  
✅ **Resume List** - View all uploaded resumes with metadata  
✅ **Advanced Search** - Search resumes by skills, location, name, experience, education, and more  
✅ **Modern UI** - Beautiful, responsive design with gradient themes  

## Technologies Used

- **Java 17**
- **Spring Boot 3.5.6**
- **Spring Data MongoDB**
- **Apache Tika 2.9.1** - Document parsing and conversion
- **Thymeleaf** - Template engine
- **Lombok** - Reduce boilerplate code
- **MongoDB** - NoSQL database

## Project Structure

```
src/main/java/com/kjr/rpf/
├── controller/
│   ├── HomeController.java          # Root redirect
│   └── ResumeController.java        # Resume CRUD operations & search
├── dto/
│   └── ResumeSearchCriteria.java    # Search criteria DTO
├── model/
│   ├── Resume.java                  # MongoDB document model
│   ├── Education.java               # Education details
│   ├── Experience.java              # Work experience
│   └── Skills.java                  # Skills information
├── repository/
│   └── ResumeRepository.java        # MongoDB repository with search methods
└── service/
    └── ResumeService.java           # Business logic & Tika conversion

src/main/resources/
├── templates/
│   ├── upload.html                  # Upload page
│   ├── viewer.html                  # Resume viewer
│   ├── list.html                    # All resumes list
│   ├── search.html                  # Search form
│   ├── search-results.html          # Search results
│   └── error.html                   # Error page
└── application.properties           # Configuration
```

## MongoDB Document Model

The `Resume` document stores:

- **Metadata**: Original filename, file type, file size, upload timestamp
- **HTML Content**: Converted HTML from Tika
- **Original Data**: Binary data of the original file for download
- **Parsed Fields**: First name, last name, email, phone, education, experience, skills

## API Endpoints

### Web Pages
- `GET /` - Redirects to upload page
- `GET /resumes/upload` - Upload page
- `POST /resumes/upload` - Upload resume (multipart/form-data)
- `GET /resumes/view/{id}` - View resume in HTML format
- `GET /resumes/list` - List all resumes
- `GET /resumes/search` - Search page
- `POST /resumes/search` - Search resumes

### REST API
- `GET /resumes/api/all` - Get all resumes (JSON)
- `GET /resumes/api/{id}` - Get resume by ID (JSON)
- `POST /resumes/api/search` - Search resumes (JSON)
- `GET /resumes/download/{id}` - Download original file
- `DELETE /resumes/api/{id}` - Delete resume

## Search Functionality

The system includes comprehensive search capabilities:

### Search Criteria
- **Personal Information**: First name, last name, full name, email, phone
- **Location**: City, state
- **Skills & Technologies**: Programming languages, frameworks, databases, tools, cloud technologies
- **Experience**: Company name, job title
- **Education**: Degree, institution, major, minimum GPA
- **Advanced**: Keyword search across all fields, date range filtering

### Search Features
- **Multi-criteria search** - Combine multiple search criteria
- **Partial matching** - Regex-based searches for flexible matching
- **Skills search** - Search across multiple skill categories
- **Date filtering** - Filter by upload date range
- **Responsive results** - Beautiful search results page with detailed resume cards

### Search Examples
- Find Java developers in California
- Search for resumes with "machine learning" experience
- Find candidates from specific universities
- Search by company name or job title

## Setup Instructions

### 1. Prerequisites
- Java 17 or higher
- Maven 3.6+
- MongoDB 4.4+ (running on localhost:27017)

### 2. MongoDB Setup

Start MongoDB:
```bash
# Windows
mongod

# Linux/Mac
sudo systemctl start mongod
```

The application will automatically create the `resume_conversions` database and collection.

### 3. Configuration

Update `src/main/resources/application.properties` if needed:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/resume_db
spring.data.mongodb.database=resume_conversions
```

### 4. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Usage

### Upload a Resume
1. Navigate to `http://localhost:8080`
2. Click or drag-and-drop a resume file
3. Click "Upload Resume"
4. You'll be redirected to the viewer page

### View Resume
- The converted HTML version displays in the viewer
- Use zoom controls to adjust the view
- Print directly from the browser

### Download Original
- Click "Download Original" button in the viewer
- The original uploaded file will be downloaded

### View All Resumes
- Click "View All Resumes" from the upload page
- See all uploaded resumes with metadata
- View, download, or delete any resume

### Search Resumes
1. Click "Search Resumes" from any page
2. Fill in your search criteria (name, location, skills, etc.)
3. Click "Search Resumes"
4. Browse through the filtered results
5. View or download matching resumes

### Search Tips
- **Skills Search**: Enter comma-separated values (e.g., "Java, Python, React")
- **Location Search**: Search by city, state, or both
- **Name Search**: Search by first name, last name, or full name
- **Keyword Search**: Searches across all resume content
- **Date Range**: Filter resumes by upload date
- **Combine Criteria**: Use multiple search fields for precise results

## Apache Tika Integration

The system uses Apache Tika to convert documents:

```java
// Automatic format detection and parsing
Parser parser = new AutoDetectParser();
parser.parse(inputStream, handler, metadata, parseContext);
```

**Supported Formats:**
- PDF (.pdf)
- Microsoft Word (.doc, .docx)
- Rich Text Format (.rtf)
- Plain Text (.txt)
- OpenDocument (.odt)
- And many more formats supported by Tika

## MongoDB Schema

```javascript
{
  "_id": ObjectId("..."),
  "originalFileName": "John_Doe_Resume.pdf",
  "originalFileType": "application/pdf",
  "originalFileSize": 245678,
  "uploadedAt": ISODate("2025-10-05T14:30:00Z"),
  "htmlContent": "<!DOCTYPE html>...",
  "originalFileData": BinData(...),
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+1-234-567-8900",
  "education": [...],
  "experience": [...],
  "skills": {...}
}
```

## Future Enhancements

- 🔍 AI-powered resume parsing to extract structured data
- 📊 Resume analytics and scoring
- 🔐 User authentication and authorization
- 📧 Email notifications
- 🌐 Multi-language support
- 📱 Mobile app integration
- 🤖 Integration with Spring AI for intelligent resume analysis

## Troubleshooting

### MongoDB Connection Error
```
Error: MongoSocketOpenException
```
**Solution:** Ensure MongoDB is running on localhost:27017

### File Upload Error
```
Error: Maximum upload size exceeded
```
**Solution:** Add to application.properties:
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Tika Parsing Error
```
Error: TikaException
```
**Solution:** Ensure the file format is supported and not corrupted

## License

This project is for demonstration purposes.

## Contact

For questions or support, please contact the development team.
