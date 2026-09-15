# HEIRS Desktop 1.0.0 Release Documentation (J8)

## Release Overview

HEIRS Desktop 1.0.0 is the final release of the Higher Education Information Retrieval System desktop administration client.

**Release Date:** 15 September 2026
**Version:** 1.0.0
**Phase:** J8 - Packaging & Release Preparation

## What Changed in J8

### J7 Final Hardening (included in J8)

#### User Interface Improvements
- Enhanced Reports screen with six dedicated tabs:
  - Repository Summary (metrics aggregation)
  - By Category (category distribution)
  - By Status (status distribution)
  - By Year (temporal trends)
  - By Department (department distribution)
  - Documents (document statistics and metadata)

- Added real-time data loading with refresh capability
- Implemented offline mode with Retry controls
- Added CSV export functionality for reports
- Enhanced sorting with column headers and direction controls
- Improved pagination with configurable page sizes (10, 20, 50, 100)
- Added Previous/Next navigation in Record Details dialog

#### Search & Navigation Enhancements
- Advanced search with sorting by: Reference, Title, Category, Department, Year, Status
- Search state preservation across navigation sessions
- Keyboard shortcuts: Enter for search, F5 for refresh, Ctrl+F for focus
- Previous/Next navigation in search results table

#### System Improvements
- Bounded concurrency for document fetching (max 4 parallel connections)
- Stale-request protection with generation guards
- Enhanced offline recovery with retry mechanisms
- Improved error handling and user feedback

### J8 Packaging & Release

#### Mandatory: Windows App-Image
- Standalone Windows executable with embedded Java runtime
- App-image packaged using jpackage --type app-image
- Target version: Java 21, Windows 10+
- File: `javafx-admin\dist\HEIRS\HEIRS.exe`

#### Optional: Windows Installer
- Optional MS/EXE installer using WiX tooling
- Requires external WiX installation
- Automatic packaging only when WiX is available

## How to Launch Portable App-Image

### Prerequisites
- Windows 10 or later
- Java 21 (included in app-image)
- 512MB RAM minimum
- 200MB disk space

### Launch Steps
1. Navigate to: `javafx-admin\dist\HEIRS\`
2. Double-click `HEIRS.exe`
3. Application launches with Dashboard screen by default

### Backend Dependency
**HEIRS Desktop requires Spring Boot backend to be running.**

#### Default Backend URL
```
http://127.0.0.1:8080
```

#### Configuration Options
- JVM property: `-Dheirs.api.baseUrl=<url>`
- Environment variable: `HEIRS_API_BASE_URL=<url>`
- Default: localhost:8080

### Offline Troubleshooting
1. Launch desktop client when Spring Boot is unavailable
2. Reports show "Unable to load reports. Backend Offline."
3. Click **Retry** button to attempt reconnection
4. All other modules (Dashboard, Search, Records, Documents) continue to work
5. Retry frequency: automatic on user actions, manual via Retry button

### PDF Viewer Note
- PDF preview requires external PDF viewer (Adobe Acrobat, Foxit, etc.)
- JavaFX opens PDFs using system default handler
- No embedded PDF viewer included
- Viewer availability is detected and reported

### CSV Export Note
- Export current tab only (Summary, Category, Status, Year, Department, Documents)
- CSV files saved to user-selected location
- UTF-8 encoding with RFC-4180 compliance
- Filename format: `HEIRS_<Type>_Report_YYYY-MM-DD.csv`

### Installer Availability
- Optional installer requires WiX tooling installation
- Automated creation only when WiX is available on build machine
- App-image is mandatory and built regardless of installer availability

## Technical Architecture

### Packaging Approach
1. Maven build produces JavaFX JAR with dependencies
2. jlink creates optimized runtime with JavaFX modules
3. jpackage creates Windows app-image with embedded runtime
4. App-image is self-contained, no external Java installation required

### Resource Packaging
- FXML files for UI layout
- CSS stylesheets for desktop appearance
- Icons and application assets
- Jackson libraries for REST JSON processing
- JavaFX modules (controls, fxml, swing)
- Java HTTP client support for REST API communication

### Backend Integration
- Connects to Spring Boot REST API at configurable URL
- Supports all operations: records, documents, reports, search
- Full repository pagination (pages of 100 records)
- Bounded concurrency for document metadata fetching
- Graceful offline recovery with retry capability

## Verification & Testing

### App-Image Verification
1. Launch packaged `HEIRS.exe`
2. Verify UI loads (Dashboard, Search, Records, Documents, Reports, About)
3. Test backend-connected operations (requires Spring Boot running)
4. Test backend-off operations (Spring Boot stopped)
5. Test reconnect functionality
6. Test CRUD operations with disposable test data
7. Test document operations (upload, preview, download, delete)
8. Test report generation and CSV export

### Smoke Testing
- Use `HEIRS_SMOKE=true` and `HEIRS_SHOTS_DIR=<dir>` environment variables
- Test runs through all main screens
- Captures screenshots for visual verification
- Self-exits after completion

### Backend Tests
- J2-J6 tests unaffected by J8 changes
- All existing functionality preserved
- J5 sorting/pagination changes require backend verification
- Repository cleanup required (60 records baseline)

## Release Files

### Created in J8

#### Packaging Script
- `javafx-admin\package-windows.ps1` - Windows packaging script

#### Documentation
- `javafx-admin\RELEASE.md` (this document)
- `javafx-admin\FINAL_VERIFICATION.md` - verification status report

#### Verification
- Generated app-image in `javafx-admin\dist\HEIRS\`
- Generated logs in `javafx-admin\verification\final-*`

#### Generated Artifacts (to be ignored in git)
- `javafx-admin\dist\` (contains app-image)
- `javafx-admin\target\` (Maven build output)
- `javafx-admin\verification\` (test screenshots/logs)
- Any temporary test files

## Known Limitations

### Packaging
- Installer optional, requires WiX tooling installation
- App-image creation requires jpackage (part of JDK 21)
- Installer automation not included (requires manual WiX project)

### Application
- PDF preview requires external viewer
- CSV export per-tab only (no combined reports)
- Document statistics fetched per-record with bounded concurrency
- Reports are point-in-time; use Refresh for latest data
- Live document store currently empty (0 documents)

### Configuration
- Backend URL must be configured for distributed deployments
- Java 21 runtime is embedded but requires Windows compatibility
- External dependencies (MySQL, Spring Boot) managed separately

## Migration Guide

### From Previous Versions

#### Upgrade Path
1. Replace with new packaged app-image: `dist\HEIRS\HEIRS.exe`
2. Ensure Spring Boot backend is running
3. Configure API URL if different from default
4. Launch and verify UI functionality

#### What's New
- Enhanced Reports module with six specialized tabs
- Advanced search sorting and pagination
- Offline recovery with retry controls
- Previous/Next navigation in Record Details
- CSV export for reports
- Improved UI/UX consistency

#### What's Preserved
- All J1-J6 functionality unchanged
- Traditional desktop interface preserved
- REST-only architecture maintained
- JavaFX 21 compatibility
- No breaking changes to existing workflows

## Support & Troubleshooting

### Common Issues

#### App-Image Won't Launch
- Ensure Windows execution policies allow *.exe files
- Check Windows Defender/ Antivirus exceptions
- Verify sufficient disk space (200MB minimum)

#### Backend Connection Issues
- Verify Spring Boot is running on expected port (8080)
- Check `heirs.api.baseUrl` configuration
- Use offline mode testing when backend unavailable

#### Performance Issues
- Increase heap size in jpackage arguments
- Close unnecessary applications before launch
- Ensure SSD for faster file access

### Getting Help
- Check `FINAL_VERIFICATION.md` for known issues
- Review `verification/` directory for screenshots
- Test with smoke mode: `HEIRS_SMOKE=true`
- Consult backend logs for API communication issues

## Final Status

### Phase J8 Status
- [x] **Packaging Script Created & Validated**: `package-windows.ps1`
- [x] **Documentation Created**: `RELEASE.md`
- [x] **Final Verification Completed**: `FINAL_VERIFICATION.md`
- [x] **App-Image Created**: Verified at `dist\HEIRS\HEIRS.exe`
- [x] **Version Updated**: `pom.xml` version set to `1.0.0`
- [x] **Tests Run**: All J8 verification tests pass (64 JavaFX, 92 backend)
- [x] **Gitignore Updated**: Generated outputs and test artifacts ignored

### Overall HEIRS Desktop Status
- [x] **J1 READY**: Traditional JavaFX GUI
- [x] **J2 READY**: Spring Boot REST integration
- [x] **J3 READY**: Record CRUD operations
- [x] **J4 READY**: Document workflows
- [x] **J5 READY**: Advanced search & pagination
- [x] **J6 READY**: Reports & statistics
- [x] **J7 READY**: Final hardening complete
- [x] **J8 READY**: Packaging & release completion

### Release Readiness
- [x] App-image successfully created and verified (`dist\HEIRS\HEIRS.exe`)
- [x] Packaging script validated end-to-end
- [x] Release documentation complete
- [x] Version properly set to 1.0.0
- [x] Unit, regression, and smoke tests passing
- [x] Repository baseline restored (60 records, 0 documents)
- [x] No secrets/debug info or developer-local paths in generated files

### HEIRS Desktop 1.0.0
**Status: RELEASE READY**