# Document Management Module

The Document module handles the retrieval and metadata management of files uploaded during the appraisal process.

## Key Components
- **`DocumentController`**: REST endpoint for retrieving documents uploaded by the current user.
- **`AppraisalDocument`**: Entity representing the metadata of an uploaded file.
- **`AppraisalDocumentRepository`**: Data access layer for document records.

## Features

### 1. Document Retrieval
- Users can retrieve a list of all documents they have uploaded for a specific academic year.
- **Endpoint**: `GET /api/v1/appraisal-documents`
- **Security**: The system automatically filters documents based on the `currentUser()` to ensure users can only see their own files.

### 2. Document Metadata
The `AppraisalDocument` entity typically stores:
- `facultyEmail`: Owner of the document.
- `academicYear`: The appraisal cycle the document belongs to.
- `fileName`: Original name of the uploaded file.
- `fileUrl` or `filePath`: Location where the physical file is stored.
- `sectionKey`: The specific section of the appraisal form the document is attached to (e.g., "journals", "awards").
- `contentType`: MIME type of the file (e.g., "application/pdf", "image/png").

## Integration with Appraisal Workflow
Documents are usually uploaded and linked during the final submission of an appraisal in the `AppraisalService`. The `DocumentController` provides a dedicated way to query these links later, for example, to display a list of attachments in a user's dashboard or profile.
